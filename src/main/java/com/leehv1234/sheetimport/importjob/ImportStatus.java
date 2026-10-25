package com.leehv1234.sheetimport.importjob;

public enum ImportStatus {

    /** 모든 행을 반영했다. */
    SUCCESS,
    /** 일부 행만 반영했다. 나머지는 검증에 실패했다. */
    PARTIAL_SUCCESS,
    /** 모든 행이 검증에 실패해 아무것도 반영하지 않았다. */
    FAILED;

    static ImportStatus of(int totalRows, int failedCount) {
        if (failedCount == 0) {
            return SUCCESS;
        }
        return failedCount == totalRows ? FAILED : PARTIAL_SUCCESS;
    }
}
