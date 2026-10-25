package com.leehv1234.sheetimport.textbook;

import com.leehv1234.sheetimport.global.error.BusinessException;
import com.leehv1234.sheetimport.global.error.ErrorCode;
import com.leehv1234.sheetimport.spreadsheet.GoogleSheetSource;
import com.leehv1234.sheetimport.spreadsheet.google.GoogleSheetReadRequest;
import com.leehv1234.sheetimport.spreadsheet.google.GoogleSheetReader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 서책 시트를 읽어 한글 헤더를 tb_textbook 컬럼 이름으로 바꿔 돌려준다. DB에는 저장하지 않는다. */
@Tag(name = "서책")
@RestController
@RequestMapping("/api/textbooks")
@RequiredArgsConstructor
public class TextbookSheetController {

    /** Google Sheets 설정이 없으면 빈이 없으므로, 필요할 때 꺼내 쓰도록 ObjectProvider로 받는다. */
    private final ObjectProvider<GoogleSheetReader> readerProvider;
    private final TextbookSheetMapper mapper;

    @Operation(summary = "Google 스프레드시트에서 서책 목록 읽기 (저장하지 않음)")
    @PostMapping("/google-sheets/read")
    public TextbookSheetResponse readGoogleSheet(@Valid @RequestBody GoogleSheetReadRequest request) {
        GoogleSheetReader reader = readerProvider.getIfAvailable();
        if (reader == null) {
            throw new BusinessException(ErrorCode.GOOGLE_SHEETS_NOT_CONFIGURED);
        }
        return mapper.map(reader.read(new GoogleSheetSource(request.spreadsheetId(), request.range())));
    }
}
