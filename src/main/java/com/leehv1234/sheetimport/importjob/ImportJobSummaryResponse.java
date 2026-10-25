package com.leehv1234.sheetimport.importjob;

import com.leehv1234.sheetimport.spreadsheet.SheetSourceType;

import java.time.LocalDateTime;

/** 목록용. 실패 행 목록은 빼고 건수만 담는다. */
public record ImportJobSummaryResponse(
        Long id,
        SheetSourceType sourceType,
        String sourceRef,
        ImportStatus status,
        int totalRows,
        int createdCount,
        int updatedCount,
        int unchangedCount,
        int failedCount,
        LocalDateTime createdAt) {

    public static ImportJobSummaryResponse from(ImportJob job) {
        return new ImportJobSummaryResponse(job.getId(), job.getSourceType(), job.getSourceRef(), job.getStatus(),
                job.getTotalRows(), job.getCreatedCount(), job.getUpdatedCount(), job.getUnchangedCount(),
                job.getFailedCount(), job.getCreatedAt());
    }
}
