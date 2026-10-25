package com.leehv1234.sheetimport.spreadsheet.excel;

import com.leehv1234.sheetimport.global.error.BusinessException;
import com.leehv1234.sheetimport.global.error.ErrorCode;
import com.leehv1234.sheetimport.spreadsheet.ExcelFileSource;
import com.leehv1234.sheetimport.spreadsheet.SheetRow;
import com.leehv1234.sheetimport.spreadsheet.SheetTable;
import com.leehv1234.sheetimport.support.ExcelFixture;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExcelSheetReaderTest {

    private final ExcelSheetReader reader = new ExcelSheetReader();

    @Test
    void 첫_행을_헤더로_읽고_빈_행은_건너뛴다() {
        byte[] file = ExcelFixture.xlsx("상품목록",
                new Object[]{"상품코드", " 상품명 ", "가격"},
                new Object[]{"A-001", "연필", 1000},
                new Object[]{},
                new Object[]{"A-002", "지우개"});

        SheetTable table = reader.read(source("products.xlsx", file, null));

        assertThat(table.headers()).containsExactly("상품코드", "상품명", "가격");
        assertThat(table.rows()).extracting(SheetRow::rowNumber).containsExactly(2, 4);
        assertThat(table.rows().get(0).get("가격")).isEqualTo("1000");   // 1000.0 이 아니라 보이는 그대로
        assertThat(table.rows().get(1).get("가격")).isEmpty();           // 셀이 없으면 빈 문자열
    }

    @Test
    void 수식은_계산된_값으로_서식이_있는_숫자는_보이는_그대로_읽는다() throws IOException {
        byte[] file;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("단가");
            header.createCell(1).setCellValue("수량");
            header.createCell(2).setCellValue("합계");

            CellStyle thousands = workbook.createCellStyle();
            thousands.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
            Row row = sheet.createRow(1);
            Cell price = row.createCell(0);
            price.setCellValue(1500);
            price.setCellStyle(thousands);
            row.createCell(1).setCellValue(3);
            row.createCell(2).setCellFormula("A2*B2");

            workbook.write(out);
            file = out.toByteArray();
        }

        SheetRow row = reader.read(source("calc.xlsx", file, null)).rows().getFirst();

        assertThat(row.get("단가")).isEqualTo("1,500");
        assertThat(row.get("합계")).isEqualTo("4500");
    }

    @Test
    void 시트_이름을_지정하면_그_시트를_읽는다() {
        byte[] file = ExcelFixture.xlsx("두번째", new Object[]{"상품코드"}, new Object[]{"B-001"});

        SheetTable table = reader.read(source("products.xlsx", file, "두번째"));

        assertThat(table.rows()).singleElement().extracting(row -> row.get("상품코드")).isEqualTo("B-001");
    }

    @Test
    void 없는_시트_이름이면_SHEET_NOT_FOUND() {
        byte[] file = ExcelFixture.xlsx("상품목록", new Object[]{"상품코드"});

        assertErrorCode(() -> reader.read(source("products.xlsx", file, "없는시트")), ErrorCode.SHEET_NOT_FOUND);
    }

    @Test
    void 엑셀_확장자가_아니면_UNSUPPORTED_FILE_TYPE() {
        assertErrorCode(() -> reader.read(source("products.csv", new byte[]{1, 2, 3}, null)),
                ErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void 확장자만_xlsx인_다른_파일이면_INVALID_FILE() {
        byte[] notExcel = "상품코드,상품명\nA-001,연필".getBytes();

        assertErrorCode(() -> reader.read(source("products.xlsx", notExcel, null)), ErrorCode.INVALID_FILE);
    }

    private static ExcelFileSource source(String filename, byte[] content, String sheetName) {
        return new ExcelFileSource(filename, new ByteArrayResource(content), sheetName);
    }

    private static void assertErrorCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }
}
