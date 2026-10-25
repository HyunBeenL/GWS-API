package com.leehv1234.sheetimport.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** 시트에 있는 상품코드들을 IN 쿼리 한 번으로 조회한다. (행마다 조회하면 N번 쿼리가 나간다) */
    List<Product> findAllBySkuIn(Collection<String> skus);
}
