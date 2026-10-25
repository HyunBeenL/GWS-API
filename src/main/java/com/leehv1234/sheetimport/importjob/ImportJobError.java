package com.leehv1234.sheetimport.importjob;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 검증에 실패한 행. {@link ImportJob}에 딸린 값이라 별도 ID 없이 import_job_error 테이블에 저장된다. */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImportJobError {

    static final int MESSAGE_MAX_LENGTH = 500;

    @Column(nullable = false)
    private int rowNo;

    @Column(nullable = false, length = MESSAGE_MAX_LENGTH)
    private String message;

    ImportJobError(int rowNo, String message) {
        this.rowNo = rowNo;
        this.message = ImportJob.truncate(message, MESSAGE_MAX_LENGTH);
    }
}
