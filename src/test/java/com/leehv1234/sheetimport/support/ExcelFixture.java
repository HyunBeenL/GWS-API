package com.leehv1234.sheetimport.support;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** 테스트용 .xlsx 파일을 메모리에서 만든다. */
public final class ExcelFixture {

    private ExcelFixture() {
    }

    /**
     * 첫 번째 배열이 헤더 행이다. 값이 {@link Number}면 숫자 셀, null이면 셀을 만들지 않는다.
     * 빈 배열을 넣으면 셀이 하나도 없는 빈 행이 된다.
     */
    public static byte[] xlsx(String sheetName, Object[]... rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < rows[r].length; c++) {
                    Object value = rows[r][c];
                    if (value instanceof Number number) {
                        row.createCell(c).setCellValue(number.doubleValue());
                    } else if (value != null) {
                        row.createCell(c).setCellValue(value.toString());
                    }
                }
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
