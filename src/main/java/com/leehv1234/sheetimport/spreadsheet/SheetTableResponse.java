package com.leehv1234.sheetimport.spreadsheet;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 읽은 표를 그대로 돌려주는 응답. 각 행의 셀은 헤더 순서대로 담는다. */
public record SheetTableResponse(List<String> headers, List<Row> rows) {

    public record Row(int rowNumber, Map<String, String> cells) {
    }

    public static SheetTableResponse from(SheetTable table) {
        List<Row> rows = table.rows().stream()
                .map(row -> new Row(row.rowNumber(), cellsInHeaderOrder(table.headers(), row)))
                .toList();
        return new SheetTableResponse(table.headers(), rows);
    }

    /** {@link SheetRow}의 셀은 순서가 없는 Map이라, 응답에서는 헤더 순서를 지키도록 다시 담는다. */
    private static Map<String, String> cellsInHeaderOrder(List<String> headers, SheetRow row) {
        Map<String, String> cells = new LinkedHashMap<>();
        headers.forEach(header -> cells.put(header, row.get(header)));
        return cells;
    }
}
