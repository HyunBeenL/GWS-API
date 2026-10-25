package com.leehv1234.sheetimport.product;

import com.leehv1234.sheetimport.global.error.BusinessException;
import com.leehv1234.sheetimport.global.error.ErrorCode;
import com.leehv1234.sheetimport.spreadsheet.MappingResult;
import com.leehv1234.sheetimport.spreadsheet.RowError;
import com.leehv1234.sheetimport.spreadsheet.SheetRow;
import com.leehv1234.sheetimport.spreadsheet.SheetTable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductRowMapperTest {

    private static final List<String> HEADERS = List.of("상품코드", "상품명", "카테고리", "가격", "재고");

    private final ProductRowMapper mapper = new ProductRowMapper();

    @Test
    void 천_단위_구분자와_통화_표시가_있어도_숫자로_바꾼다() {
        SheetTable table = table(HEADERS, row(2, "A-001", "연필", "문구", "₩1,000", "1,200"));

        MappingResult<ProductRow> result = mapper.map(table);

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).singleElement().satisfies(product -> {
            assertThat(product.sku()).isEqualTo("A-001");
            assertThat(product.category()).isEqualTo("문구");
            assertThat(product.price()).isEqualByComparingTo("1000");
            assertThat(product.stock()).isEqualTo(1200);
        });
    }

    @Test
    void 선택_컬럼이_없으면_카테고리는_null_재고는_0() {
        List<String> headers = List.of("상품코드", "상품명", "가격");

        MappingResult<ProductRow> result = mapper.map(table(headers, SheetRow.of(2, headers, List.of("A-001", "연필", "500"))));

        assertThat(result.rows()).singleElement().satisfies(product -> {
            assertThat(product.category()).isNull();
            assertThat(product.stock()).isZero();
        });
    }

    @Test
    void 필수_컬럼이_헤더에_없으면_시트_전체를_거부한다() {
        List<String> headers = List.of("상품코드", "상품명");
        SheetTable table = table(headers, SheetRow.of(2, headers, List.of("A-001", "연필")));

        assertThatThrownBy(() -> mapper.map(table))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MISSING_COLUMNS);
                    assertThat(e.getMessage()).contains("가격");
                });
    }

    @Test
    void 잘못된_행은_행_번호와_이유를_모으고_나머지는_통과시킨다() {
        SheetTable table = table(HEADERS,
                row(2, "A-001", "연필", "문구", "1000", "10"),
                row(3, "A-002", "", "문구", "abc", "1"),
                row(4, "A-003", "자", "문구", "300", "1.5"),
                row(5, "A-004", "공책", "문구", "12.345", "-1"),
                row(6, "A-001", "연필", "문구", "1000", "10"));

        MappingResult<ProductRow> result = mapper.map(table);

        assertThat(result.totalRows()).isEqualTo(5);
        assertThat(result.rows()).extracting(ProductRow::rowNumber).containsExactly(2);
        assertThat(result.errors()).extracting(RowError::rowNumber).containsExactly(3, 4, 5, 6);
        assertThat(result.errors().get(0).message()).contains("[상품명] 값이 비어 있습니다", "[가격] 숫자가 아닙니다: abc");
        assertThat(result.errors().get(1).message()).contains("[재고] 정수여야 합니다");
        assertThat(result.errors().get(2).message()).contains("소수점 둘째 자리", "[재고] 0 이상");
        assertThat(result.errors().get(3).message()).contains("2행과 중복됩니다");
    }

    @Test
    void 헤더만_있고_데이터가_없으면_거부한다() {
        assertThatThrownBy(() -> mapper.map(table(HEADERS)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NO_DATA_ROWS));
    }

    private static SheetTable table(List<String> headers, SheetRow... rows) {
        return SheetTable.of(headers, Arrays.asList(rows));
    }

    private static SheetRow row(int rowNumber, String... values) {
        return SheetRow.of(rowNumber, HEADERS, List.of(values));
    }
}
