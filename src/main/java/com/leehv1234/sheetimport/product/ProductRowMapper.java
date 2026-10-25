package com.leehv1234.sheetimport.product;

import com.leehv1234.sheetimport.global.error.BusinessException;
import com.leehv1234.sheetimport.global.error.ErrorCode;
import com.leehv1234.sheetimport.spreadsheet.MappingResult;
import com.leehv1234.sheetimport.spreadsheet.RowError;
import com.leehv1234.sheetimport.spreadsheet.SheetRow;
import com.leehv1234.sheetimport.spreadsheet.SheetTable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.leehv1234.sheetimport.product.ProductColumn.CATEGORY;
import static com.leehv1234.sheetimport.product.ProductColumn.NAME;
import static com.leehv1234.sheetimport.product.ProductColumn.PRICE;
import static com.leehv1234.sheetimport.product.ProductColumn.SKU;
import static com.leehv1234.sheetimport.product.ProductColumn.STOCK;

/**
 * 시트의 각 행을 {@link ProductRow}로 바꾸면서 값을 검증한다.
 *
 * <p>잘못된 행이 하나 있다고 전체를 실패시키지 않는다. 잘못된 행은 행 번호·이유와 함께 모아 두고,
 * 통과한 행만 저장 단계로 넘긴다. 반면 필수 컬럼이 헤더에 아예 없으면 양식 자체가 틀린 것이므로 바로 거부한다.
 */
@Component
public class ProductRowMapper {

    private static final int SKU_MAX_LENGTH = 50;
    private static final int NAME_MAX_LENGTH = 200;
    private static final int CATEGORY_MAX_LENGTH = 100;
    /** price 컬럼 DECIMAL(15, 2)의 정수부 자릿수 */
    private static final int PRICE_MAX_INTEGER_DIGITS = 13;
    private static final int PRICE_MAX_SCALE = 2;
    /** 숫자 셀에 섞여 오는 천 단위 구분자, 통화 표시, 공백 */
    private static final Pattern NUMBER_NOISE = Pattern.compile("[,\\s₩원]");

    public MappingResult<ProductRow> map(SheetTable table) {
        requireColumns(table);
        if (table.rows().isEmpty()) {
            throw new BusinessException(ErrorCode.NO_DATA_ROWS);
        }

        List<ProductRow> rows = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();
        Map<String, Integer> firstRowNumberBySku = new HashMap<>();

        for (SheetRow row : table.rows()) {
            List<String> problems = new ArrayList<>();
            String sku = requiredText(row, SKU, SKU_MAX_LENGTH, problems);
            String name = requiredText(row, NAME, NAME_MAX_LENGTH, problems);
            String category = optionalText(row, CATEGORY, CATEGORY_MAX_LENGTH, problems);
            BigDecimal price = price(row, problems);
            int stock = stock(row, problems);

            if (sku != null) {
                Integer firstRowNumber = firstRowNumberBySku.putIfAbsent(sku, row.rowNumber());
                if (firstRowNumber != null) {
                    problems.add("%s %d행과 중복됩니다: %s".formatted(SKU.label(), firstRowNumber, sku));
                }
            }

            if (problems.isEmpty()) {
                rows.add(new ProductRow(row.rowNumber(), sku, name, category, price, stock));
            } else {
                errors.add(new RowError(row.rowNumber(), String.join(" / ", problems)));
            }
        }
        return new MappingResult<>(table.rows().size(), rows, errors);
    }

    private static void requireColumns(SheetTable table) {
        List<String> missing = Arrays.stream(ProductColumn.values())
                .filter(ProductColumn::isRequired)
                .map(ProductColumn::getHeader)
                .filter(header -> !table.headers().contains(header))
                .toList();
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_COLUMNS,
                    "필수 컬럼이 없습니다: %s (시트의 헤더: %s)".formatted(String.join(", ", missing), table.headers()));
        }
    }

    private static String requiredText(SheetRow row, ProductColumn column, int maxLength, List<String> problems) {
        String value = row.get(column.getHeader());
        if (value.isEmpty()) {
            problems.add(column.label() + " 값이 비어 있습니다");
            return null;
        }
        return checkLength(value, column, maxLength, problems);
    }

    private static String optionalText(SheetRow row, ProductColumn column, int maxLength, List<String> problems) {
        String value = row.get(column.getHeader());
        return value.isEmpty() ? null : checkLength(value, column, maxLength, problems);
    }

    private static String checkLength(String value, ProductColumn column, int maxLength, List<String> problems) {
        if (value.length() > maxLength) {
            problems.add("%s %d자를 넘을 수 없습니다".formatted(column.label(), maxLength));
            return null;
        }
        return value;
    }

    private static BigDecimal price(SheetRow row, List<String> problems) {
        String raw = row.get(PRICE.getHeader());
        if (raw.isEmpty()) {
            problems.add(PRICE.label() + " 값이 비어 있습니다");
            return null;
        }
        BigDecimal price = parseNumber(raw);
        if (price == null) {
            problems.add(PRICE.label() + " 숫자가 아닙니다: " + raw);
            return null;
        }
        if (price.signum() < 0) {
            problems.add(PRICE.label() + " 0 이상이어야 합니다: " + raw);
            return null;
        }
        if (price.stripTrailingZeros().scale() > PRICE_MAX_SCALE) {
            problems.add(PRICE.label() + " 소수점 둘째 자리까지만 입력할 수 있습니다: " + raw);
            return null;
        }
        if (price.precision() - price.scale() > PRICE_MAX_INTEGER_DIGITS) {
            problems.add(PRICE.label() + " 값이 너무 큽니다: " + raw);
            return null;
        }
        return price;
    }

    /** 재고 컬럼은 선택이다. 비어 있으면 0으로 본다. */
    private static int stock(SheetRow row, List<String> problems) {
        String raw = row.get(STOCK.getHeader());
        if (raw.isEmpty()) {
            return 0;
        }
        BigDecimal number = parseNumber(raw);
        if (number == null) {
            problems.add(STOCK.label() + " 숫자가 아닙니다: " + raw);
            return 0;
        }
        try {
            int stock = number.intValueExact();
            if (stock < 0) {
                problems.add(STOCK.label() + " 0 이상이어야 합니다: " + raw);
            }
            return stock;
        } catch (ArithmeticException e) {
            problems.add(STOCK.label() + " 정수여야 합니다: " + raw);
            return 0;
        }
    }

    private static BigDecimal parseNumber(String raw) {
        try {
            return new BigDecimal(NUMBER_NOISE.matcher(raw).replaceAll(""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
