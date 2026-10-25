package com.leehv1234.sheetimport.importjob;

import com.leehv1234.sheetimport.spreadsheet.SheetSourceType;

import java.time.LocalDateTime;
import java.util.List;

/** 가져오기 결과 상세. 실패한 행과 이유를 함께 돌려준다. */
public record ImportJobResponse(
        Long id,
        SheetSourceType sourceType,
        String sourceRef,
        ImportStatus status,
        int totalRows,
        int createdCount,
        int updatedCount,
        int unchangedCount,
        int failedCount,
        LocalDateTime createdAt,
        List<RowErrorResponse> errors) {

    public static ImportJobResponse from(ImportJob job) {
        return new ImportJobResponse(job.getId(), job.getSourceType(), job.getSourceRef(), job.getStatus(),
                job.getTotalRows(), job.getCreatedCount(), job.getUpdatedCount(), job.getUnchangedCount(),
                job.getFailedCount(), job.getCreatedAt(),
                job.getErrors().stream().map(RowErrorResponse::from).toList());
    }

    public record RowErrorResponse(int rowNumber, String message) {

        static RowErrorResponse from(ImportJobError error) {
            return new RowErrorResponse(error.getRowNo(), error.getMessage());
        }
    }
}
