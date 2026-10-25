package com.leehv1234.sheetimport.spreadsheet.google;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.leehv1234.sheetimport.global.error.BusinessException;
import com.leehv1234.sheetimport.global.error.ErrorCode;
import com.leehv1234.sheetimport.spreadsheet.GoogleSheetSource;
import com.leehv1234.sheetimport.spreadsheet.SheetReader;
import com.leehv1234.sheetimport.spreadsheet.SheetRow;
import com.leehv1234.sheetimport.spreadsheet.SheetTable;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sheets API의 {@code spreadsheets.values.get}으로 범위의 값을 읽는다.
 *
 * <p>기본 응답 형식(FORMATTED_VALUE)은 시트 화면에 보이는 문자열이라 엑셀 쪽과 같은 형태가 된다.
 * 응답에서 행 끝의 빈 셀은 생략되므로 {@link SheetRow#of}가 빈 문자열로 채운다.
 *
 * <p>설정이 있을 때만 {@link GoogleSheetsConfig}에서 빈으로 등록한다.
 */
@RequiredArgsConstructor
public class GoogleSheetReader implements SheetReader<GoogleSheetSource> {

    /** {@code 'Sheet1'!A3:E10} 에서 시작 행(3)을 찾는다. */
    private static final Pattern START_ROW = Pattern.compile("^[A-Za-z]*(\\d+)");

    private final Sheets sheets;

    @Override
    public SheetTable read(GoogleSheetSource source) {
        ValueRange response = fetch(source);
        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_SHEET);
        }

        // 범위가 1행이 아닌 곳에서 시작할 수 있으므로, 응답에 담긴 실제 범위로 행 번호를 맞춘다.
        int headerRowNumber = startRow(response.getRange());
        List<String> headers = toStrings(values.getFirst());
        List<SheetRow> rows = new ArrayList<>(values.size() - 1);
        for (int i = 1; i < values.size(); i++) {
            rows.add(SheetRow.of(headerRowNumber + i, headers, toStrings(values.get(i))));
        }
        return SheetTable.of(headers, rows);
    }

    private ValueRange fetch(GoogleSheetSource source) {
        try {
            return sheets.spreadsheets().values()
                    .get(source.spreadsheetId(), source.range())
                    .execute();
        } catch (GoogleJsonResponseException e) {
            throw translate(e);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.GOOGLE_SHEETS_API_ERROR, e);
        }
    }

    private static BusinessException translate(GoogleJsonResponseException e) {
        ErrorCode code = switch (e.getStatusCode()) {
            case 400 -> ErrorCode.GOOGLE_SHEETS_BAD_REQUEST;
            case 403 -> ErrorCode.GOOGLE_SHEETS_ACCESS_DENIED;
            case 404 -> ErrorCode.GOOGLE_SHEETS_NOT_FOUND;
            default -> ErrorCode.GOOGLE_SHEETS_API_ERROR;
        };
        String reason = e.getDetails() != null ? e.getDetails().getMessage() : e.getStatusMessage();
        return new BusinessException(code, code.getMessage() + " (Google 응답: " + reason + ")", e);
    }

    static int startRow(String a1Range) {
        if (a1Range == null) {
            return 1;
        }
        String cells = a1Range.substring(a1Range.lastIndexOf('!') + 1);
        Matcher matcher = START_ROW.matcher(cells);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 1;
    }

    private static List<String> toStrings(List<Object> row) {
        return row.stream().map(value -> Objects.toString(value, "")).toList();
    }
}
