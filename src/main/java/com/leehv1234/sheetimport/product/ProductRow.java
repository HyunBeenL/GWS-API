package com.leehv1234.sheetimport.product;

import java.math.BigDecimal;

/** 검증을 통과한 시트 한 행. */
public record ProductRow(int rowNumber, String sku, String name, String category, BigDecimal price, int stock) {

    Product toEntity() {
        return new Product(sku, name, category, price, stock);
    }
}
