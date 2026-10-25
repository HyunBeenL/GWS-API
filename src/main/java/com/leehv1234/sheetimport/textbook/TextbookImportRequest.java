package com.leehv1234.sheetimport.textbook;

import com.leehv1234.sheetimport.spreadsheet.GoogleSheetSource;
import com.leehv1234.sheetimport.spreadsheet.SheetRow;
import com.leehv1234.sheetimport.spreadsheet.SheetTable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Apps Script가 시트에서 읽은 값을 그대로 보내는 요청.
 *
 * <p>{@code values}는 {@code sheet.getDataRange().getDisplayValues()} 결과라 A1부터 시작하고,
 * 시트 화면에 보이는 문자열이다. 그래서 {@code values[i]}는 시트의 {@code i + 1}행이다.
 */
public record TextbookImportRequest(

        @Schema(description = "스프레드시트 ID 또는 URL. 가져오기 이력에 출처로 남는다",
                example = "1AbCdEfGhIjKlMnOpQrStUvWxYz")
        @NotBlank
        String spreadsheetId,

        @Schema(description = "시트(탭) 이름. 가져오기 이력에 출처로 남는다", example = "서책목록")
        String sheetName,

        @Schema(description = "A1부터 읽은 시트 값. 첫 행은 헤더, 나머지는 데이터",
                example = "[[\"서책코드\", \"서책명\", \"출판사\"], [\"TB-E-KOR-3-1\", \"국어 3-1\", \"천재교육\"]]")
        @NotEmpty
        List<List<String>> values) {

    /** 서버가 시트를 직접 읽지는 않지만, 이력에는 어느 스프레드시트의 어느 시트였는지 남긴다. */
    public GoogleSheetSource source() {
        return new GoogleSheetSource(spreadsheetId, sheetName);
    }

    public SheetTable toTable() {
        List<String> headers = cells(values.getFirst());
        List<SheetRow> rows = new ArrayList<>(values.size() - 1);
        for (int i = 1; i < values.size(); i++) {
            rows.add(SheetRow.of(i + 1, headers, cells(values.get(i))));
        }
        return SheetTable.of(headers, rows);
    }

    /** JSON에서 null로 온 행·셀은 빈 값으로 본다. */
    private static List<String> cells(List<String> row) {
        return row == null ? List.of() : row.stream().map(cell -> Objects.toString(cell, "")).toList();
    }
}
