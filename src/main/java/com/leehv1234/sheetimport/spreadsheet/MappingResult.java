package com.leehv1234.sheetimport.spreadsheet;

import java.util.List;

/**
 * 표를 도메인 값으로 바꾼 결과. 통과한 행과 실패한 행을 함께 담는다.
 *
 * @param totalRows 검사한 데이터 행 수 (= rows + errors)
 */
public record MappingResult<T>(int totalRows, List<T> rows, List<RowError> errors) {

    public MappingResult {
        rows = List.copyOf(rows);
        errors = List.copyOf(errors);
    }
}
