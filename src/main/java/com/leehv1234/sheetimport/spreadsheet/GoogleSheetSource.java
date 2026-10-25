package com.leehv1234.sheetimport.spreadsheet;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Google 스프레드시트의 특정 범위.
 *
 * @param spreadsheetId 스프레드시트 ID. 브라우저 주소창의 URL을 그대로 넣어도 ID만 뽑아낸다.
 * @param range         A1 표기 범위 (예: {@code 상품목록!A1:E}). 비어 있으면 첫 번째 시트의 {@code A:Z}.
 */
public record GoogleSheetSource(String spreadsheetId, String range) implements SheetSource {

    public static final String DEFAULT_RANGE = "A:Z";

    private static final Pattern ID_IN_URL = Pattern.compile("/spreadsheets/d/([a-zA-Z0-9_-]+)");

    public GoogleSheetSource {
        spreadsheetId = extractId(Objects.requireNonNull(spreadsheetId, "spreadsheetId"));
        range = (range == null || range.isBlank()) ? DEFAULT_RANGE : range.strip();
    }

    @Override
    public SheetSourceType type() {
        return SheetSourceType.GOOGLE_SHEETS;
    }

    @Override
    public String description() {
        return spreadsheetId + " [" + range + "]";
    }

    private static String extractId(String idOrUrl) {
        Matcher matcher = ID_IN_URL.matcher(idOrUrl);
        return matcher.find() ? matcher.group(1) : idOrUrl.strip();
    }
}
