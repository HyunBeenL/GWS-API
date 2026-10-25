package com.leehv1234.sheetimport.textbook;

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

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 서책(tb_textbook). 시트의 '서책코드'(code)가 같으면 같은 서책으로 본다.
 * 컬럼 이름은 Spring Boot 기본 규칙(camelCase → snake_case)으로 맞춰진다.
 */
@Entity
@Table(name = "tb_textbook")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Textbook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long textbookId;

    @Column(length = 100)
    private String code;

    @Column(length = 200)
    private String name;

    @Column(length = 200)
    private String publisher;

    @Column(length = 200)
    private String author;

    @Column(length = 50)
    private String curriculum;

    @Column(length = 2)
    private String schoolLevel;

    @Column(length = 2)
    private String grade;

    @Column(length = 30)
    private String subject;

    @Column(length = 20)
    private String term;

    @Column(length = 1)
    private String useYn;

    @Column(length = 100)
    private String showDepth;

    @Column(length = 1)
    private String viewTextbookYn;

    @Column(length = 1)
    private String viewSpecialYn;

    @Column(length = 1)
    private String viewResourceYn;

    /** 로그인 기능이 없어 아직 채우지 않는다. */
    private Long creatorId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createDate;

    /** 로그인 기능이 없어 아직 채우지 않는다. */
    private Long updaterId;

    @UpdateTimestamp
    private LocalDateTime updateDate;

    /** 값이 있으면 삭제된 서책. 가져오기는 삭제되지 않은 서책만 갱신한다. */
    private LocalDateTime deletedAt;

    Textbook(TextbookRow row) {
        row.values().forEach(this::set);
    }

    /**
     * 시트 값으로 덮어쓴다. 값이 하나라도 달라졌을 때만 필드를 바꾸고 true를 돌려준다.
     * 아무것도 바꾸지 않으면 JPA 변경 감지가 UPDATE를 보내지 않는다. 시트에 헤더가 없는 컬럼은 그대로 둔다.
     */
    boolean update(TextbookRow row) {
        boolean changed = row.values().entrySet().stream()
                .anyMatch(entry -> !Objects.equals(get(entry.getKey()), entry.getValue()));
        if (changed) {
            row.values().forEach(this::set);
        }
        return changed;
    }

    /** switch 식이라 {@link TextbookColumn}에 컬럼을 추가하면 여기서 컴파일 에러가 난다. {@link #set}도 같이 고칠 것. */
    private String get(TextbookColumn column) {
        return switch (column) {
            case CODE -> code;
            case NAME -> name;
            case PUBLISHER -> publisher;
            case AUTHOR -> author;
            case CURRICULUM -> curriculum;
            case SCHOOL_LEVEL -> schoolLevel;
            case GRADE -> grade;
            case SUBJECT -> subject;
            case TERM -> term;
            case USE_YN -> useYn;
            case SHOW_DEPTH -> showDepth;
            case VIEW_TEXTBOOK_YN -> viewTextbookYn;
            case VIEW_SPECIAL_YN -> viewSpecialYn;
            case VIEW_RESOURCE_YN -> viewResourceYn;
        };
    }

    private void set(TextbookColumn column, String value) {
        switch (column) {
            case CODE -> code = value;
            case NAME -> name = value;
            case PUBLISHER -> publisher = value;
            case AUTHOR -> author = value;
            case CURRICULUM -> curriculum = value;
            case SCHOOL_LEVEL -> schoolLevel = value;
            case GRADE -> grade = value;
            case SUBJECT -> subject = value;
            case TERM -> term = value;
            case USE_YN -> useYn = value;
            case SHOW_DEPTH -> showDepth = value;
            case VIEW_TEXTBOOK_YN -> viewTextbookYn = value;
            case VIEW_SPECIAL_YN -> viewSpecialYn = value;
            case VIEW_RESOURCE_YN -> viewResourceYn = value;
        }
    }
}
