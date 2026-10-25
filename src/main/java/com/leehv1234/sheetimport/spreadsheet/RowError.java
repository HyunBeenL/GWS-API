package com.leehv1234.sheetimport.spreadsheet;

/** 검증에 실패한 행과 그 이유. */
public record RowError(int rowNumber, String message) {
}
