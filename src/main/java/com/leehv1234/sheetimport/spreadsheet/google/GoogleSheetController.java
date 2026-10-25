package com.leehv1234.sheetimport.spreadsheet.google;

import com.leehv1234.sheetimport.global.error.BusinessException;
import com.leehv1234.sheetimport.global.error.ErrorCode;
import com.leehv1234.sheetimport.spreadsheet.GoogleSheetSource;
import com.leehv1234.sheetimport.spreadsheet.SheetTableResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Google 스프레드시트의 범위를 읽어 헤더와 행을 그대로 돌려준다. DB에는 저장하지 않는다. */
@Tag(name = "Google 스프레드시트")
@RestController
@RequestMapping("/api/google-sheets")
@RequiredArgsConstructor
public class GoogleSheetController {

    /** Google Sheets 설정이 없으면 빈이 없으므로, 필요할 때 꺼내 쓰도록 ObjectProvider로 받는다. */
    private final ObjectProvider<GoogleSheetReader> readerProvider;

    @Operation(summary = "Google 스프레드시트 범위 읽기 (저장하지 않음)")
    @PostMapping("/read")
    public SheetTableResponse read(@Valid @RequestBody GoogleSheetReadRequest request) {
        GoogleSheetReader reader = readerProvider.getIfAvailable();
        if (reader == null) {
            throw new BusinessException(ErrorCode.GOOGLE_SHEETS_NOT_CONFIGURED);
        }
        return SheetTableResponse.from(reader.read(new GoogleSheetSource(request.spreadsheetId(), request.range())));
    }
}
