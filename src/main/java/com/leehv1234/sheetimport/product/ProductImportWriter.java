package com.leehv1234.sheetimport.product;

import com.leehv1234.sheetimport.importjob.ImportJob;
import com.leehv1234.sheetimport.importjob.ImportJobRepository;
import com.leehv1234.sheetimport.importjob.ImportJobResponse;
import com.leehv1234.sheetimport.spreadsheet.MappingResult;
import com.leehv1234.sheetimport.spreadsheet.SheetSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 검증을 통과한 행을 상품 테이블에 upsert(있으면 수정, 없으면 생성)하고 가져오기 이력을 남긴다.
 *
 * <p>상품 저장과 이력 저장을 한 트랜잭션으로 묶어, 둘 중 하나만 반영되는 일이 없게 한다.
 * {@link ProductImportService}와 클래스를 나눈 이유는 {@code @Transactional}이 프록시로 동작하기 때문이다.
 * 같은 클래스 안에서 메서드를 호출하면 프록시를 거치지 않아 트랜잭션이 걸리지 않는다.
 */
@Component
@RequiredArgsConstructor
class ProductImportWriter {

    private final ProductRepository productRepository;
    private final ImportJobRepository importJobRepository;

    @Transactional
    public ImportJobResponse write(SheetSource source, MappingResult<ProductRow> result) {
        List<ProductRow> rows = result.rows();
        Map<String, Product> existing = findExisting(rows);

        List<Product> newProducts = new ArrayList<>();
        int updated = 0;
        int unchanged = 0;
        for (ProductRow row : rows) {
            Product product = existing.get(row.sku());
            if (product == null) {
                newProducts.add(row.toEntity());
            } else if (product.update(row.name(), row.category(), row.price(), row.stock())) {
                updated++;   // 수정은 save 없이 트랜잭션 커밋 시점에 변경 감지로 UPDATE 된다.
            } else {
                unchanged++;
            }
        }
        productRepository.saveAll(newProducts);

        ImportJob job = importJobRepository.save(
                ImportJob.of(source, result.totalRows(), newProducts.size(), updated, unchanged, result.errors()));
        return ImportJobResponse.from(job);
    }

    private Map<String, Product> findExisting(List<ProductRow> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        List<String> skus = rows.stream().map(ProductRow::sku).toList();
        return productRepository.findAllBySkuIn(skus).stream()
                .collect(Collectors.toMap(Product::getSku, Function.identity()));
    }
}
