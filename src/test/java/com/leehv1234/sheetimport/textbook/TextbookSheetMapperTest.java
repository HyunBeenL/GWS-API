package com.leehv1234.sheetimport.textbook;

import com.leehv1234.sheetimport.global.error.BusinessException;
import com.leehv1234.sheetimport.global.error.ErrorCode;
import com.leehv1234.sheetimport.spreadsheet.MappingResult;
import com.leehv1234.sheetimport.spreadsheet.RowError;
import com.leehv1234.sheetimport.spreadsheet.SheetRow;
import com.leehv1234.sheetimport.spreadsheet.SheetTable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.leehv1234.sheetimport.textbook.TextbookColumn.AUTHOR;
import static com.leehv1234.sheetimport.textbook.TextbookColumn.CODE;
import static com.leehv1234.sheetimport.textbook.TextbookColumn.NAME;
import static com.leehv1234.sheetimport.textbook.TextbookColumn.USE_YN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

class TextbookSheetMapperTest {

    private final TextbookSheetMapper mapper = new TextbookSheetMapper();

    @Test
    void 한글_헤더를_tb_textbook_컬럼_이름으로_바꾸고_시트에_있는_컬럼만_담는다() {
        List<String> headers = List.of("저자", "서책명", "서책코드");
        SheetTable table = SheetTable.of(headers, List.of(
                SheetRow.of(2, headers, List.of("홍길동", "국어 3-1", "TB-001")),
                SheetRow.of(4, headers, List.of("", "수학 3-1", "TB-002"))));

        TextbookSheetResponse response = mapper.map(table);

        assertThat(response.rows()).extracting(TextbookSheetResponse.Row::rowNumber).containsExactly(2, 4);
        // 시트의 헤더 순서와 상관없이 tb_textbook 컬럼 순서로 담는다.
        assertThat(response.rows().get(0).values())
                .containsExactly(entry("code", "TB-001"), entry("name", "국어 3-1"), entry("author", "홍길동"));
        assertThat(response.rows().get(1).values()).containsEntry("author", "");
        assertThat(response.ignoredHeaders()).isEmpty();
    }

    @Test
    void 정의에_없는_헤더는_무시하고_ignoredHeaders로_알려준다() {
        List<String> headers = List.of("서책코드", "서책명", "비고", "출판 사");
        SheetTable table = SheetTable.of(headers, List.of(SheetRow.of(2, headers, List.of("TB-001", "국어 3-1", "메모", "천재교육"))));

        TextbookSheetResponse response = mapper.map(table);

        assertThat(response.rows().get(0).values()).containsOnlyKeys("code", "name");
        assertThat(response.ignoredHeaders()).containsExactly("비고", "출판 사");
    }

    @Test
    void 필수_헤더가_없으면_시트_전체를_거부한다() {
        List<String> headers = List.of("서책코드", "저자");
        SheetTable table = SheetTable.of(headers, List.of(SheetRow.of(2, headers, List.of("TB-001", "홍길동"))));

        assertThatThrownBy(() -> mapper.map(table))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MISSING_COLUMNS);
                    assertThat(e.getMessage()).contains("서책명");
                });
    }

    @Test
    void 저장용_변환은_빈_셀을_null로_YN을_대문자로_바꾸고_시트에_있는_컬럼만_담는다() {
        List<String> headers = List.of("서책코드", "서책명", "저자", "사용여부");
        SheetTable table = SheetTable.of(headers, List.of(SheetRow.of(2, headers, List.of("TB-001", "국어 3-1", "", "y"))));

        MappingResult<TextbookRow> result = mapper.toRows(table);

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).singleElement().satisfies(row -> {
            assertThat(row.rowNumber()).isEqualTo(2);
            assertThat(row.code()).isEqualTo("TB-001");
            assertThat(row.values())
                    .containsOnlyKeys(CODE, NAME, AUTHOR, USE_YN)
                    .containsEntry(AUTHOR, null)
                    .containsEntry(USE_YN, "Y");
        });
    }

    @Test
    void 저장용_변환은_잘못된_행만_이유와_함께_빼고_나머지는_통과시킨다() {
        List<String> headers = List.of("서책코드", "서책명", "학교급", "사용여부");
        SheetTable table = SheetTable.of(headers, List.of(
                SheetRow.of(2, headers, List.of("TB-001", "국어 3-1", "초", "Y")),
                SheetRow.of(3, headers, List.of("TB-002", "", "초등학교", "예")),
                SheetRow.of(4, headers, List.of("TB-001", "국어 3-2", "", ""))));

        MappingResult<TextbookRow> result = mapper.toRows(table);

        assertThat(result.totalRows()).isEqualTo(3);
        assertThat(result.rows()).extracting(TextbookRow::rowNumber).containsExactly(2);
        assertThat(result.errors()).extracting(RowError::rowNumber).containsExactly(3, 4);
        assertThat(result.errors().get(0).message())
                .contains("서책명: 값이 비어 있습니다", "학교급: 2자를 넘을 수 없습니다", "사용여부: Y 또는 N만");
        assertThat(result.errors().get(1).message()).contains("서책코드: 2행과 중복됩니다");
    }

    @Test
    void 저장용_변환은_데이터_행이_없으면_거부한다() {
        SheetTable table = SheetTable.of(List.of("서책코드", "서책명"), List.of());

        assertThatThrownBy(() -> mapper.toRows(table))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NO_DATA_ROWS));
    }
}
