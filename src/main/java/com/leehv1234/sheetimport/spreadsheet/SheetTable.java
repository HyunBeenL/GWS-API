package com.leehv1234.sheetimport.spreadsheet;

import java.util.List;

/**
 * 소스(엑셀, Google Sheets)와 상관없이 "첫 행은 헤더, 나머지는 데이터"로 정규화한 표.
 * 도메인 로직은 이 형태만 알면 되므로, 소스가 늘어나도 도메인 코드는 바뀌지 않는다.
 *
 * @param headers 비어 있지 않은 헤더 이름 (앞뒤 공백 제거)
 * @param rows    모든 셀이 빈 행을 뺀 데이터 행
 */
public record SheetTable(List<String> headers, List<SheetRow> rows) {

    public SheetTable {
        headers = List.copyOf(headers);
        rows = List.copyOf(rows);
    }

    public static SheetTable of(List<String> headerCells, List<SheetRow> rows) {
        List<String> headers = headerCells.stream()
                .map(String::strip)
                .filter(header -> !header.isEmpty())
                .distinct()
                .toList();
        List<SheetRow> dataRows = rows.stream()
                .filter(row -> !row.isBlank())
                .toList();
        return new SheetTable(headers, dataRows);
    }
}
