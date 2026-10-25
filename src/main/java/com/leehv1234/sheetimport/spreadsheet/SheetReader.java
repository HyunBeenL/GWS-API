package com.leehv1234.sheetimport.spreadsheet;

/**
 * 소스 {@code S}를 읽어 {@link SheetTable}로 돌려준다.
 * 읽을 수 없으면(파일 손상, 권한 없음 등) {@link com.leehv1234.sheetimport.global.error.BusinessException}을 던진다.
 */
public interface SheetReader<S extends SheetSource> {

    SheetTable read(S source);
}
