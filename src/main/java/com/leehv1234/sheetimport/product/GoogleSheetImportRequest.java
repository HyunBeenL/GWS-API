package com.leehv1234.sheetimport.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record GoogleSheetImportRequest(

        @Schema(description = "스프레드시트 ID 또는 브라우저 주소창의 URL",
                example = "https://docs.google.com/spreadsheets/d/1AbCdEfGhIjKlMnOpQrStUvWxYz/edit")
        @NotBlank
        String spreadsheetId,

        @Schema(description = "A1 표기 범위. 비우면 첫 번째 시트의 A:Z", example = "상품목록!A1:E")
        String range) {
}
