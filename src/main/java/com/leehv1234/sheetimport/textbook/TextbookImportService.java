package com.leehv1234.sheetimport.textbook;

import com.leehv1234.sheetimport.importjob.ImportJobResponse;
import com.leehv1234.sheetimport.spreadsheet.MappingResult;
import com.leehv1234.sheetimport.spreadsheet.SheetSource;
import com.leehv1234.sheetimport.spreadsheet.SheetTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 서책 가져오기 흐름: ① 행을 검증·변환하고 → ② 한 트랜잭션으로 저장한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TextbookImportService {

    private final TextbookSheetMapper mapper;
    private final TextbookImportWriter writer;

    /**
     * @param source 가져오기 이력에 남길 출처
     * @param table  이미 읽어 둔 표. 서버가 시트를 읽지 않고 클라이언트(Apps Script)가 보낸 값을 그대로 쓴다.
     */
    public ImportJobResponse importTable(SheetSource source, SheetTable table) {
        MappingResult<TextbookRow> result = mapper.toRows(table);
        ImportJobResponse response = writer.write(source, result);

        log.info("서책 가져오기 #{} {} - {}: 전체 {}, 생성 {}, 수정 {}, 변경없음 {}, 실패 {}",
                response.id(), response.status(), source.description(), response.totalRows(),
                response.createdCount(), response.updatedCount(), response.unchangedCount(), response.failedCount());
        return response;
    }
}
