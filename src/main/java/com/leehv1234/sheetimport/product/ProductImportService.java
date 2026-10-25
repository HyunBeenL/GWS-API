package com.leehv1234.sheetimport.product;

import com.leehv1234.sheetimport.global.error.BusinessException;
import com.leehv1234.sheetimport.global.error.ErrorCode;
import com.leehv1234.sheetimport.importjob.ImportJobResponse;
import com.leehv1234.sheetimport.spreadsheet.ExcelFileSource;
import com.leehv1234.sheetimport.spreadsheet.GoogleSheetSource;
import com.leehv1234.sheetimport.spreadsheet.MappingResult;
import com.leehv1234.sheetimport.spreadsheet.SheetSource;
import com.leehv1234.sheetimport.spreadsheet.SheetTable;
import com.leehv1234.sheetimport.spreadsheet.excel.ExcelSheetReader;
import com.leehv1234.sheetimport.spreadsheet.google.GoogleSheetReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * 상품 가져오기 흐름: ① 소스에서 표를 읽고 → ② 행을 검증·변환하고 → ③ 한 트랜잭션으로 저장한다.
 *
 * <p>①은 파일·네트워크 I/O라 시간이 걸릴 수 있으므로 트랜잭션 밖에서 처리한다.
 * 트랜잭션 안에서 Google API 응답을 기다리면 그동안 DB 커넥션을 붙잡고 있게 된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImportService {

    private final ExcelSheetReader excelSheetReader;
    /** Google Sheets 설정이 없으면 빈이 없으므로, 필요할 때 꺼내 쓰도록 ObjectProvider로 받는다. */
    private final ObjectProvider<GoogleSheetReader> googleSheetReaderProvider;
    private final ProductRowMapper rowMapper;
    private final ProductImportWriter writer;

    public ImportJobResponse importFrom(SheetSource source) {
        SheetTable table = read(source);
        MappingResult<ProductRow> result = rowMapper.map(table);
        ImportJobResponse response = writer.write(source, result);

        log.info("상품 가져오기 #{} {} - {}: 전체 {}, 생성 {}, 수정 {}, 변경없음 {}, 실패 {}",
                response.id(), response.status(), source.description(), response.totalRows(),
                response.createdCount(), response.updatedCount(), response.unchangedCount(), response.failedCount());
        return response;
    }

    private SheetTable read(SheetSource source) {
        // SheetSource가 sealed라서 모든 경우를 다루지 않으면 컴파일되지 않는다.
        return switch (source) {
            case ExcelFileSource excel -> excelSheetReader.read(excel);
            case GoogleSheetSource googleSheet -> googleSheetReader().read(googleSheet);
        };
    }

    private GoogleSheetReader googleSheetReader() {
        GoogleSheetReader reader = googleSheetReaderProvider.getIfAvailable();
        if (reader == null) {
            throw new BusinessException(ErrorCode.GOOGLE_SHEETS_NOT_CONFIGURED);
        }
        return reader;
    }
}
