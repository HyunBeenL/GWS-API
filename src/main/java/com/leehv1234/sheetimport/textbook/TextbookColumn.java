package com.leehv1234.sheetimport.textbook;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 서책 시트 첫 행(헤더)에 올 수 있는 컬럼과 대응하는 tb_textbook 컬럼.
 * 시트 양식이 바뀌면 여기 헤더 이름만 고치면 된다. 순서는 상관없고, 이 밖의 컬럼은 무시한다.
 */
@Getter
@RequiredArgsConstructor
public enum TextbookColumn {

    CODE("서책코드", "code", true, 100),
    NAME("서책명", "name", true, 200),
    PUBLISHER("출판사", "publisher", false, 200),
    AUTHOR("저자", "author", false, 200),
    CURRICULUM("교육과정", "curriculum", false, 50),
    SCHOOL_LEVEL("학교급", "school_level", false, 2),
    GRADE("학년", "grade", false, 2),
    SUBJECT("과목", "subject", false, 30),
    TERM("학기", "term", false, 20),
    USE_YN("사용여부", "use_yn", false, 1),
    SHOW_DEPTH("표시깊이", "show_depth", false, 100),
    VIEW_TEXTBOOK_YN("교과서보기여부", "view_textbook_yn", false, 1),
    VIEW_SPECIAL_YN("특별자료보기여부", "view_special_yn", false, 1),
    VIEW_RESOURCE_YN("자료보기여부", "view_resource_yn", false, 1);

    /** 시트의 헤더 이름. 앞뒤 공백을 뺀 뒤 글자가 정확히 같아야 인식한다. */
    private final String header;
    /** tb_textbook의 컬럼 이름 */
    private final String column;
    /** true면 헤더에 이 컬럼이 없을 때 시트 전체를 거부하고, 값이 빈 행은 저장하지 않는다. */
    private final boolean required;
    /** tb_textbook 컬럼의 VARCHAR 길이. 넘는 값이 있는 행은 저장하지 않는다. */
    private final int maxLength;

    /** Y/N만 들어가는 컬럼. 소문자 y/n도 받아서 대문자로 저장한다. */
    public boolean isYesNo() {
        return column.endsWith("_yn");
    }
}
