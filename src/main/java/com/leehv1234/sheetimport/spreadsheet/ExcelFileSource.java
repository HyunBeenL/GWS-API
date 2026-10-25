package com.leehv1234.sheetimport.spreadsheet;

import org.springframework.core.io.InputStreamSource;

import java.util.Objects;

/**
 * 업로드된 엑셀 파일.
 *
 * @param filename  원본 파일명 (확장자 검사에 쓴다)
 * @param content   파일 내용. {@code MultipartFile}도 {@link InputStreamSource}라서 그대로 넘길 수 있다.
 * @param sheetName 읽을 시트 이름. 비어 있으면 첫 번째 시트를 읽는다.
 */
public record ExcelFileSource(String filename, InputStreamSource content, String sheetName) implements SheetSource {

    public ExcelFileSource {
        Objects.requireNonNull(content, "content");
    }

    @Override
    public SheetSourceType type() {
        return SheetSourceType.EXCEL;
    }

    @Override
    public String description() {
        String name = Objects.requireNonNullElse(filename, "(이름 없음)");
        return hasSheetName() ? name + " [" + sheetName + "]" : name;
    }

    public boolean hasSheetName() {
        return sheetName != null && !sheetName.isBlank();
    }
}
