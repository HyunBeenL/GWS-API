package com.leehv1234.sheetimport.textbook;

import java.util.List;
import java.util.Map;

/**
 * 서책 시트를 읽어 tb_textbook 컬럼 이름으로 바꾼 결과. 저장 전에 확인하는 용도다.
 *
 * @param ignoredHeaders {@link TextbookColumn}에 없어 무시한 시트 헤더. 헤더 오타를 찾을 때 본다.
 */
public record TextbookSheetResponse(List<Row> rows, List<String> ignoredHeaders) {

    /** @param values tb_textbook 컬럼 이름 → 셀 값. 시트에 있는 컬럼만 담는다. */
    public record Row(int rowNumber, Map<String, String> values) {
    }
}
