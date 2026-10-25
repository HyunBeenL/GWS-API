package com.leehv1234.sheetimport.product;

import com.leehv1234.sheetimport.global.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "상품")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;

    @Operation(summary = "상품 목록 (기본: 상품코드순)")
    @GetMapping
    public PageResponse<ProductResponse> list(@ParameterObject @PageableDefault(sort = "sku") Pageable pageable) {
        return PageResponse.from(productRepository.findAll(pageable).map(ProductResponse::from));
    }
}
