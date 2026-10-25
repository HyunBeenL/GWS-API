package com.leehv1234.sheetimport.spreadsheet.excel;

import com.leehv1234.sheetimport.global.error.BusinessException;
import com.leehv1234.sheetimport.global.error.ErrorCode;
import com.leehv1234.sheetimport.spreadsheet.ExcelFileSource;
import com.leehv1234.sheetimport.spreadsheet.SheetReader;
import com.leehv1234.sheetimport.spreadsheet.SheetRow;
import com.leehv1234.sheetimport.spreadsheet.SheetTable;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Apache POI로 엑셀 파일을 읽는다.
 *
 * <p>셀 값은 {@link DataFormatter}로 "엑셀 화면에 보이는 그대로"의 문자열로 꺼낸다.
 * 숫자 1000이 {@code 1000.0}으로 읽히거나 수식이 {@code A2*B2} 같은 원문으로 읽히는 일을 막고,
 * Google Sheets(FORMATTED_VALUE)와 같은 형태의 값을 만들어 도메인 쪽 변환 로직을 하나로 유지한다.
 *
 * <p>파일 전체를 메모리에 올리므로 업로드 크기는 {@code spring.servlet.multipart.max-file-size}로 제한한다.
 */
@Component
public class ExcelSheetReader implements SheetReader<ExcelFileSource> {

    private static final Set<String> EXTENSIONS = Set.of("xlsx", "xls");

    @Override
    public SheetTable read(ExcelFileSource source) {
        validateExtension(source.filename());
        try (InputStream in = source.content().getInputStream();
             Workbook workbook = WorkbookFactory.create(in)) {
            return toTable(selectSheet(workbook, source), workbook);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            // 엑셀이 아닌 파일, 손상된 파일, 암호가 걸린 파일(EncryptedDocumentException) 등
            throw new BusinessException(ErrorCode.INVALID_FILE, e);
        }
    }

    private static void validateExtension(String filename) {
        String extension = StringUtils.getFilenameExtension(filename);
        if (extension == null || !EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    private static Sheet selectSheet(Workbook workbook, ExcelFileSource source) {
        if (!source.hasSheetName()) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new BusinessException(ErrorCode.EMPTY_SHEET);
            }
            return workbook.getSheetAt(0);
        }
        Sheet sheet = workbook.getSheet(source.sheetName().strip());
        if (sheet == null) {
            throw new BusinessException(ErrorCode.SHEET_NOT_FOUND, "시트를 찾을 수 없습니다: " + source.sheetName());
        }
        return sheet;
    }

    private static SheetTable toTable(Sheet sheet, Workbook workbook) {
        DataFormatter formatter = new DataFormatter();
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

        // rowIterator는 한 번도 쓰지 않은 행을 건너뛰므로, 실제 행 번호는 row.getRowNum()으로 구한다.
        Iterator<Row> rows = sheet.rowIterator();
        if (!rows.hasNext()) {
            throw new BusinessException(ErrorCode.EMPTY_SHEET);
        }
        Row headerRow = rows.next();
        int columnCount = Math.max(headerRow.getLastCellNum(), 0);
        List<String> headers = cellValues(headerRow, columnCount, formatter, evaluator);

        List<SheetRow> dataRows = new ArrayList<>();
        while (rows.hasNext()) {
            Row row = rows.next();
            dataRows.add(SheetRow.of(row.getRowNum() + 1, headers, cellValues(row, columnCount, formatter, evaluator)));
        }
        return SheetTable.of(headers, dataRows);
    }

    private static List<String> cellValues(Row row, int columnCount, DataFormatter formatter, FormulaEvaluator evaluator) {
        List<String> values = new ArrayList<>(columnCount);
        for (int column = 0; column < columnCount; column++) {
            // 셀이 없으면(null) 빈 문자열, 수식이면 계산 결과를 서식대로 돌려준다.
            values.add(formatter.formatCellValue(row.getCell(column), evaluator));
        }
        return values;
    }
}
