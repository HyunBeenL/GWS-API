package com.leehv1234.sheetimport.textbook;

import com.leehv1234.sheetimport.spreadsheet.GoogleSheetSource;
import com.leehv1234.sheetimport.spreadsheet.SheetRow;
import com.leehv1234.sheetimport.spreadsheet.SheetTable;
import com.leehv1234.sheetimport.spreadsheet.google.GoogleSheetReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 웹 계층만 띄워 요청/응답 형식과 예외 → ProblemDetail 변환을 검증한다. 시트 리더는 가짜(mock)다. */
@WebMvcTest(TextbookSheetController.class)
@Import(TextbookSheetMapper.class)
class TextbookSheetControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GoogleSheetReader reader;

    @Test
    void 한글_헤더를_tb_textbook_컬럼_이름으로_바꿔_돌려준다() throws Exception {
        List<String> headers = List.of("서책코드", "서책명", "비고");
        given(reader.read(new GoogleSheetSource("1AbC", "Sheet1!A1:C")))
                .willReturn(SheetTable.of(headers, List.of(SheetRow.of(2, headers, List.of("TB-001", "국어 3-1", "메모")))));

        mockMvc.perform(post("/api/textbooks/google-sheets/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"spreadsheetId": "https://docs.google.com/spreadsheets/d/1AbC/edit", "range": "Sheet1!A1:C"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].rowNumber").value(2))
                .andExpect(jsonPath("$.rows[0].values.code").value("TB-001"))
                .andExpect(jsonPath("$.rows[0].values.name").value("국어 3-1"))
                .andExpect(jsonPath("$.ignoredHeaders[0]").value("비고"));
    }

    @Test
    void 필수_헤더가_없으면_400_ProblemDetail로_거부한다() throws Exception {
        List<String> headers = List.of("서책코드");
        given(reader.read(any())).willReturn(SheetTable.of(headers, List.of(SheetRow.of(2, headers, List.of("TB-001")))));

        mockMvc.perform(post("/api/textbooks/google-sheets/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"spreadsheetId": "1AbC"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MISSING_COLUMNS"));
    }
}
