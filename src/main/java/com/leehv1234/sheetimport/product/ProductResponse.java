package com.leehv1234.sheetimport.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String category,
        BigDecimal price,
        int stock,
        LocalDateTime updatedAt) {

    static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getSku(), product.getName(), product.getCategory(),
                product.getPrice(), product.getStock(), product.getUpdatedAt());
    }
}
