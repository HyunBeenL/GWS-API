package com.leehv1234.sheetimport.product;

import com.leehv1234.sheetimport.global.error.BusinessException;
import com.leehv1234.sheetimport.global.error.ErrorCode;
import com.leehv1234.sheetimport.importjob.ImportJobResponse;
import com.leehv1234.sheetimport.spreadsheet.ExcelFileSource;
import com.leehv1234.sheetimport.spreadsheet.GoogleSheetSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

/**
 * 가져오기를 실행하면 이력(import job)이 하나 생기므로 201 Created와 이력 조회 주소(Location)를 돌려준다.
 * 일부 행이 검증에 실패해도 나머지는 반영되며, 실패한 행은 응답의 {@code errors}에 담긴다.
 */
@Tag(name = "상품 가져오기")
@RestController
@RequestMapping("/api/products/imports")
@RequiredArgsConstructor
public class ProductImportController {

    private final ProductImportService importService;

    @Operation(summary = "엑셀 파일(.xlsx, .xls) 업로드로 상품 가져오기")
    @PostMapping(value = "/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportJobResponse> importExcel(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String sheetName) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_FILE);
        }
        return created(importService.importFrom(new ExcelFileSource(file.getOriginalFilename(), file, sheetName)));
    }

    @Operation(summary = "Google 스프레드시트에서 상품 가져오기")
    @PostMapping("/google-sheets")
    public ResponseEntity<ImportJobResponse> importGoogleSheet(@Valid @RequestBody GoogleSheetImportRequest request) {
        return created(importService.importFrom(new GoogleSheetSource(request.spreadsheetId(), request.range())));
    }

    private static ResponseEntity<ImportJobResponse> created(ImportJobResponse body) {
        return ResponseEntity.created(URI.create("/api/import-jobs/" + body.id())).body(body);
    }
}
