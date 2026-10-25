package com.leehv1234.sheetimport.product;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시트 첫 행(헤더)에 와야 하는 컬럼. 시트 양식이 바뀌면 여기 헤더 이름만 고치면 된다.
 * 순서는 상관없고, 이 밖의 컬럼이 더 있어도 무시한다.
 */
@Getter
@RequiredArgsConstructor
public enum ProductColumn {

    SKU("상품코드", true),
    NAME("상품명", true),
    CATEGORY("카테고리", false),
    PRICE("가격", true),
    STOCK("재고", false);

    private final String header;
    /** true면 헤더에 이 컬럼이 없을 때 시트 전체를 거부한다. */
    private final boolean required;

    String label() {
        return "[" + header + "]";
    }
}
