package com.leehv1234.sheetimport.spreadsheet.google;

import com.leehv1234.sheetimport.global.error.BusinessException;
import com.leehv1234.sheetimport.global.error.ErrorCode;
import com.leehv1234.sheetimport.spreadsheet.GoogleSheetSource;
import com.leehv1234.sheetimport.spreadsheet.SheetRow;
import com.leehv1234.sheetimport.spreadsheet.SheetTable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 웹 계층만 띄워 요청/응답 형식과 예외 → ProblemDetail 변환을 검증한다. 시트 리더는 가짜(mock)다. */
@WebMvcTest(GoogleSheetController.class)
class GoogleSheetControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GoogleSheetReader reader;

    @Test
    void 읽은_표를_헤더_순서대로_돌려준다() throws Exception {
        List<String> headers = List.of("name", "code");
        given(reader.read(new GoogleSheetSource("1AbC", "Sheet1!A1:B")))
                .willReturn(SheetTable.of(headers, List.of(SheetRow.of(2, headers, List.of("Math", "T-001")))));

        mockMvc.perform(post("/api/google-sheets/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"spreadsheetId": "https://docs.google.com/spreadsheets/d/1AbC/edit", "range": "Sheet1!A1:B"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headers", contains("name", "code")))
                .andExpect(jsonPath("$.rows[0].rowNumber").value(2))
                .andExpect(content().string(containsString("""
                        "cells":{"name":"Math","code":"T-001"}""")));
    }

    @Test
    void 스프레드시트_ID가_없으면_400() throws Exception {
        mockMvc.perform(post("/api/google-sheets/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"range": "Sheet1!A1:B"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 리더_예외는_ErrorCode의_상태코드와_코드로_응답한다() throws Exception {
        given(reader.read(any())).willThrow(new BusinessException(ErrorCode.GOOGLE_SHEETS_ACCESS_DENIED));

        mockMvc.perform(post("/api/google-sheets/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"spreadsheetId": "1AbC"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("GOOGLE_SHEETS_ACCESS_DENIED"));
    }
}
