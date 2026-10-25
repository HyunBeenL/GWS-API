package com.leehv1234.sheetimport.textbook;

import com.leehv1234.sheetimport.importjob.ImportJob;
import com.leehv1234.sheetimport.importjob.ImportJobRepository;
import com.leehv1234.sheetimport.importjob.ImportJobResponse;
import com.leehv1234.sheetimport.spreadsheet.MappingResult;
import com.leehv1234.sheetimport.spreadsheet.SheetSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 검증을 통과한 행을 tb_textbook에 upsert(있으면 수정, 없으면 생성)하고 가져오기 이력을 남긴다.
 *
 * <p>서책 저장과 이력 저장을 한 트랜잭션으로 묶어, 둘 중 하나만 반영되는 일이 없게 한다.
 * {@link TextbookImportService}와 클래스를 나눈 이유는 {@code @Transactional}이 프록시로 동작하기 때문이다.
 * 같은 클래스 안에서 메서드를 호출하면 프록시를 거치지 않아 트랜잭션이 걸리지 않는다.
 */
@Component
@RequiredArgsConstructor
class TextbookImportWriter {

    private final TextbookRepository textbookRepository;
    private final ImportJobRepository importJobRepository;

    @Transactional
    public ImportJobResponse write(SheetSource source, MappingResult<TextbookRow> result) {
        List<TextbookRow> rows = result.rows();
        Map<String, Textbook> existing = findExisting(rows);

        List<Textbook> newTextbooks = new ArrayList<>();
        int updated = 0;
        int unchanged = 0;
        for (TextbookRow row : rows) {
            Textbook textbook = existing.get(row.code());
            if (textbook == null) {
                newTextbooks.add(new Textbook(row));
            } else if (textbook.update(row)) {
                updated++;   // 수정은 save 없이 트랜잭션 커밋 시점에 변경 감지로 UPDATE 된다.
            } else {
                unchanged++;
            }
        }
        textbookRepository.saveAll(newTextbooks);

        ImportJob job = importJobRepository.save(
                ImportJob.of(source, result.totalRows(), newTextbooks.size(), updated, unchanged, result.errors()));
        return ImportJobResponse.from(job);
    }

    private Map<String, Textbook> findExisting(List<TextbookRow> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        List<String> codes = rows.stream().map(TextbookRow::code).toList();
        // code에 unique 제약이 없어 같은 코드가 여러 행일 수 있다. 그때는 먼저 만들어진(id가 작은) 서책을 갱신한다.
        return textbookRepository.findAllByCodeInAndDeletedAtIsNull(codes).stream()
                .collect(Collectors.toMap(Textbook::getCode, Function.identity(),
                        (first, second) -> first.getTextbookId() < second.getTextbookId() ? first : second));
    }
}
