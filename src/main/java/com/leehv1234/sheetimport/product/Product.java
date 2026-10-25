package com.leehv1234.sheetimport.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/** 샘플 도메인. 시트의 '상품코드'(sku)가 같으면 같은 상품으로 본다. */
@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 100)
    private String category;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Product(String sku, String name, String category, BigDecimal price, int stock) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
    }

    /**
     * 시트 값으로 덮어쓴다. 값이 하나라도 달라졌을 때만 필드를 바꾸고 true를 돌려준다.
     * 아무것도 바꾸지 않으면 JPA 변경 감지가 UPDATE를 보내지 않는다.
     */
    public boolean update(String name, String category, BigDecimal price, int stock) {
        boolean changed = !Objects.equals(this.name, name)
                || !Objects.equals(this.category, category)
                || this.price.compareTo(price) != 0   // 1000 과 1000.00 은 같은 값
                || this.stock != stock;
        if (changed) {
            this.name = name;
            this.category = category;
            this.price = price;
            this.stock = stock;
        }
        return changed;
    }
}
