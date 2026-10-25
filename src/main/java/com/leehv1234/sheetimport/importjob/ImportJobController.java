package com.leehv1234.sheetimport.importjob;

import com.leehv1234.sheetimport.global.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "가져오기 이력")
@RestController
@RequestMapping("/api/import-jobs")
@RequiredArgsConstructor
public class ImportJobController {

    private final ImportJobService importJobService;

    @Operation(summary = "가져오기 이력 목록 (최신순)")
    @GetMapping
    public PageResponse<ImportJobSummaryResponse> list(
            @ParameterObject @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return importJobService.list(pageable);
    }

    @Operation(summary = "가져오기 이력 상세 (실패한 행과 이유 포함)")
    @GetMapping("/{id}")
    public ImportJobResponse get(@PathVariable Long id) {
        return importJobService.get(id);
    }
}
