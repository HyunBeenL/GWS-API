package com.leehv1234.sheetimport.textbook;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * 검증을 통과한 서책 시트 한 행.
 *
 * @param values 시트에 헤더가 있는 컬럼만 담는다(빈 셀은 null). 헤더가 없는 컬럼은 저장할 때 건드리지 않는다.
 */
public record TextbookRow(int rowNumber, Map<TextbookColumn, String> values) {

    public TextbookRow {
        // Map.copyOf는 null 값을 허용하지 않으므로 EnumMap으로 복사한다.
        values = Collections.unmodifiableMap(new EnumMap<>(values));
    }

    public String code() {
        return values.get(TextbookColumn.CODE);
    }
}
