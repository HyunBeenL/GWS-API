package com.leehv1234.sheetimport.spreadsheet.google;

import com.google.api.client.json.Json;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.api.services.sheets.v4.Sheets;
import com.leehv1234.sheetimport.global.error.BusinessException;
import com.leehv1234.sheetimport.global.error.ErrorCode;
import com.leehv1234.sheetimport.spreadsheet.GoogleSheetSource;
import com.leehv1234.sheetimport.spreadsheet.SheetRow;
import com.leehv1234.sheetimport.spreadsheet.SheetTable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 실제 Google 서버 대신 google-http-client의 MockHttpTransport로 응답을 흉내 낸다.
 * 인증 없이도 "응답 JSON → SheetTable" 변환과 오류 코드 매핑을 검증할 수 있다.
 */
class GoogleSheetReaderTest {

    private static final GoogleSheetSource SOURCE = new GoogleSheetSource("sheet-id", "상품목록!A3:C");

    @Test
    void 응답_범위의_시작_행을_기준으로_행_번호를_매긴다() {
        GoogleSheetReader reader = readerRespondingWith(200, """
                {
                  "range": "'상품목록'!A3:C5",
                  "majorDimension": "ROWS",
                  "values": [
                    ["상품코드", "상품명", "가격"],
                    ["A-001", "연필", "1,000"],
                    ["A-002", "지우개"]
                  ]
                }
                """);

        SheetTable table = reader.read(SOURCE);

        assertThat(table.headers()).containsExactly("상품코드", "상품명", "가격");
        assertThat(table.rows()).extracting(SheetRow::rowNumber).containsExactly(4, 5);
        assertThat(table.rows().get(0).get("가격")).isEqualTo("1,000");
        assertThat(table.rows().get(1).get("가격")).isEmpty();   // 행 끝의 빈 셀은 응답에서 빠진다
    }

    @Test
    void 값이_없는_범위면_EMPTY_SHEET() {
        GoogleSheetReader reader = readerRespondingWith(200, """
                {"range": "Sheet1!A1:Z1000", "majorDimension": "ROWS"}
                """);

        assertErrorCode(reader, ErrorCode.EMPTY_SHEET);
    }

    @Test
    void 시트가_서비스_계정에_공유되지_않으면_ACCESS_DENIED() {
        GoogleSheetReader reader = readerRespondingWith(403, """
                {"error": {"code": 403, "message": "The caller does not have permission", "status": "PERMISSION_DENIED"}}
                """);

        assertThatThrownBy(() -> reader.read(SOURCE))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.GOOGLE_SHEETS_ACCESS_DENIED);
                    assertThat(e.getMessage()).contains("The caller does not have permission");
                });
    }

    @Test
    void 없는_스프레드시트면_NOT_FOUND() {
        GoogleSheetReader reader = readerRespondingWith(404, """
                {"error": {"code": 404, "message": "Requested entity was not found.", "status": "NOT_FOUND"}}
                """);

        assertErrorCode(reader, ErrorCode.GOOGLE_SHEETS_NOT_FOUND);
    }

    @Test
    void 범위_표기가_잘못되면_BAD_REQUEST() {
        GoogleSheetReader reader = readerRespondingWith(400, """
                {"error": {"code": 400, "message": "Unable to parse range: 없는시트!A1", "status": "INVALID_ARGUMENT"}}
                """);

        assertErrorCode(reader, ErrorCode.GOOGLE_SHEETS_BAD_REQUEST);
    }

    @Test
    void 시작_행_파싱() {
        assertThat(GoogleSheetReader.startRow("'상품 목록'!B7:E20")).isEqualTo(7);
        assertThat(GoogleSheetReader.startRow("Sheet1!A1:Z1000")).isEqualTo(1);
        assertThat(GoogleSheetReader.startRow(null)).isEqualTo(1);
    }

    private static GoogleSheetReader readerRespondingWith(int status, String json) {
        MockLowLevelHttpResponse response = new MockLowLevelHttpResponse()
                .setStatusCode(status)
                .setContentType(Json.MEDIA_TYPE)
                .setContent(json);
        MockHttpTransport transport = new MockHttpTransport.Builder().setLowLevelHttpResponse(response).build();
        Sheets sheets = new Sheets.Builder(transport, GsonFactory.getDefaultInstance(), null)
                .setApplicationName("test")
                .build();
        return new GoogleSheetReader(sheets);
    }

    private static void assertErrorCode(GoogleSheetReader reader, ErrorCode expected) {
        assertThatThrownBy(() -> reader.read(SOURCE))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }
}
