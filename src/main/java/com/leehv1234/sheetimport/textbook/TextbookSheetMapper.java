package com.leehv1234.sheetimport.textbook;

import com.leehv1234.sheetimport.global.error.BusinessException;
import com.leehv1234.sheetimport.global.error.ErrorCode;
import com.leehv1234.sheetimport.spreadsheet.MappingResult;
import com.leehv1234.sheetimport.spreadsheet.RowError;
import com.leehv1234.sheetimport.spreadsheet.SheetRow;
import com.leehv1234.sheetimport.spreadsheet.SheetTable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.leehv1234.sheetimport.textbook.TextbookColumn.CODE;

/**
 * 서책 시트의 한글 헤더를 tb_textbook 컬럼으로 바꾼다.
 *
 * <ul>
 *   <li>{@link #map} — 미리보기용. 값은 검사하지 않고 그대로 돌려준다.</li>
 *   <li>{@link #toRows} — 저장용. 행마다 값(필수, 길이, Y/N, 서책코드 중복)을 검사한다.</li>
 * </ul>
 */
@Component
public class TextbookSheetMapper {

    private static final Set<String> KNOWN_HEADERS = Arrays.stream(TextbookColumn.values())
            .map(TextbookColumn::getHeader)
            .collect(Collectors.toUnmodifiableSet());

    public TextbookSheetResponse map(SheetTable table) {
        requireColumns(table);

        List<TextbookColumn> presentColumns = presentColumns(table);
        List<TextbookSheetResponse.Row> rows = table.rows().stream()
                .map(row -> new TextbookSheetResponse.Row(row.rowNumber(), values(presentColumns, row)))
                .toList();
        List<String> ignoredHeaders = table.headers().stream()
                .filter(header -> !KNOWN_HEADERS.contains(header))
                .toList();
        return new TextbookSheetResponse(rows, ignoredHeaders);
    }

    /**
     * 잘못된 행이 하나 있다고 전체를 실패시키지 않는다. 잘못된 행은 행 번호·이유와 함께 모아 두고,
     * 통과한 행만 저장 단계로 넘긴다. 반면 필수 컬럼이 헤더에 아예 없으면 양식 자체가 틀린 것이므로 바로 거부한다.
     */
    public MappingResult<TextbookRow> toRows(SheetTable table) {
        requireColumns(table);
        if (table.rows().isEmpty()) {
            throw new BusinessException(ErrorCode.NO_DATA_ROWS);
        }

        List<TextbookColumn> presentColumns = presentColumns(table);
        List<TextbookRow> rows = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();
        Map<String, Integer> firstRowNumberByCode = new HashMap<>();

        for (SheetRow row : table.rows()) {
            List<String> problems = new ArrayList<>();
            Map<TextbookColumn, String> values = new EnumMap<>(TextbookColumn.class);
            presentColumns.forEach(column -> values.put(column, validate(column, row.get(column.getHeader()), problems)));

            String code = values.get(CODE);
            if (code != null) {
                Integer firstRowNumber = firstRowNumberByCode.putIfAbsent(code, row.rowNumber());
                if (firstRowNumber != null) {
                    problems.add("%s: %d행과 중복됩니다 (%s)".formatted(CODE.getHeader(), firstRowNumber, code));
                }
            }

            if (problems.isEmpty()) {
                rows.add(new TextbookRow(row.rowNumber(), values));
            } else {
                errors.add(new RowError(row.rowNumber(), String.join(" / ", problems)));
            }
        }
        return new MappingResult<>(table.rows().size(), rows, errors);
    }

    private static void requireColumns(SheetTable table) {
        List<String> missing = Arrays.stream(TextbookColumn.values())
                .filter(TextbookColumn::isRequired)
                .map(TextbookColumn::getHeader)
                .filter(header -> !table.headers().contains(header))
                .toList();
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_COLUMNS,
                    "필수 컬럼이 없습니다: %s (시트의 헤더: %s)".formatted(String.join(", ", missing), table.headers()));
        }
    }

    /** enum 순서(= tb_textbook 컬럼 순서)대로, 시트에 헤더가 있는 컬럼만 */
    private static List<TextbookColumn> presentColumns(SheetTable table) {
        return Arrays.stream(TextbookColumn.values())
                .filter(column -> table.headers().contains(column.getHeader()))
                .toList();
    }

    private static Map<String, String> values(List<TextbookColumn> columns, SheetRow row) {
        Map<String, String> values = new LinkedHashMap<>();
        columns.forEach(column -> values.put(column.getColumn(), row.get(column.getHeader())));
        return values;
    }

    /** 빈 셀은 null로 바꾼다. 문제가 있으면 이유를 problems에 더하고 null을 돌려준다. */
    private static String validate(TextbookColumn column, String raw, List<String> problems) {
        if (raw.isEmpty()) {
            if (column.isRequired()) {
                problems.add(column.getHeader() + ": 값이 비어 있습니다");
            }
            return null;
        }
        if (column.isYesNo()) {
            String yesNo = raw.toUpperCase(Locale.ROOT);
            if (!yesNo.equals("Y") && !yesNo.equals("N")) {
                problems.add("%s: Y 또는 N만 입력할 수 있습니다 (%s)".formatted(column.getHeader(), raw));
                return null;
            }
            return yesNo;
        }
        if (raw.length() > column.getMaxLength()) {
            problems.add("%s: %d자를 넘을 수 없습니다 (%s)".formatted(column.getHeader(), column.getMaxLength(), raw));
            return null;
        }
        return raw;
    }
}
