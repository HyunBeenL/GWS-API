package com.leehv1234.sheetimport.textbook;

import com.leehv1234.sheetimport.importjob.ImportJobResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * 가져오기를 실행하면 이력(import job)이 하나 생기므로 201 Created와 이력 조회 주소(Location)를 돌려준다.
 * 일부 행이 검증에 실패해도 나머지는 반영되며, 실패한 행은 응답의 {@code errors}에 담긴다.
 */
@Tag(name = "서책")
@RestController
@RequestMapping("/api/textbooks/imports")
@RequiredArgsConstructor
public class TextbookImportController {

    private final TextbookImportService importService;

    @Operation(summary = "시트 값을 직접 받아 서책 저장 (Apps Script용)",
            description = "서책코드가 같은 서책이 있으면 수정, 없으면 생성한다. 시트에 헤더가 없는 컬럼은 건드리지 않는다.")
    @PostMapping("/values")
    public ResponseEntity<ImportJobResponse> importValues(@Valid @RequestBody TextbookImportRequest request) {
        ImportJobResponse body = importService.importTable(request.source(), request.toTable());
        return ResponseEntity.created(URI.create("/api/import-jobs/" + body.id())).body(body);
    }
}
