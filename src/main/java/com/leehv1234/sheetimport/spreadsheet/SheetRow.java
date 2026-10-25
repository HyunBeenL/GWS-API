package com.leehv1234.sheetimport.spreadsheet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 데이터 한 행. 셀 값은 헤더 이름으로 꺼낸다.
 *
 * @param rowNumber 사용자가 시트에서 보는 실제 행 번호(1부터). 오류 메시지에 그대로 쓴다.
 * @param cells     헤더 이름 → 셀 값(앞뒤 공백 제거, 빈 셀은 빈 문자열)
 */
public record SheetRow(int rowNumber, Map<String, String> cells) {

    public SheetRow {
        cells = Map.copyOf(cells);
    }

    /**
     * 같은 위치의 헤더와 값을 짝지어 행을 만든다.
     * 헤더가 빈 열은 버리고, 값이 모자라면(행 끝의 빈 셀) 빈 문자열로 채운다.
     */
    public static SheetRow of(int rowNumber, List<String> headerCells, List<String> values) {
        Map<String, String> cells = new HashMap<>();
        for (int i = 0; i < headerCells.size(); i++) {
            String header = headerCells.get(i).strip();
            if (header.isEmpty()) {
                continue;
            }
            String value = i < values.size() ? values.get(i).strip() : "";
            cells.putIfAbsent(header, value);
        }
        return new SheetRow(rowNumber, cells);
    }

    /** 헤더에 없는 컬럼이면 빈 문자열을 돌려준다. */
    public String get(String header) {
        return cells.getOrDefault(header, "");
    }

    public boolean isBlank() {
        return cells.values().stream().allMatch(String::isEmpty);
    }
}
