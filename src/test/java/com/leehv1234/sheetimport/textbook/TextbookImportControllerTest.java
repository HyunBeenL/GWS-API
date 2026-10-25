package com.leehv1234.sheetimport.textbook;

import com.leehv1234.sheetimport.importjob.ImportJobResponse;
import com.leehv1234.sheetimport.importjob.ImportStatus;
import com.leehv1234.sheetimport.spreadsheet.GoogleSheetSource;
import com.leehv1234.sheetimport.spreadsheet.SheetSource;
import com.leehv1234.sheetimport.spreadsheet.SheetSourceType;
import com.leehv1234.sheetimport.spreadsheet.SheetTable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 웹 계층만 띄워 Apps Script가 보내는 요청 → 표 변환과 응답 형식을 검증한다. 서비스는 가짜(mock)다. */
@WebMvcTest(TextbookImportController.class)
class TextbookImportControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TextbookImportService importService;

    @Test
    void 시트_값을_표로_바꿔_넘기고_201과_이력_주소를_돌려준다() throws Exception {
        given(importService.importTable(any(), any())).willReturn(new ImportJobResponse(7L, SheetSourceType.GOOGLE_SHEETS,
                "1AbC [서책목록]", ImportStatus.SUCCESS, 1, 1, 0, 0, 0, LocalDateTime.now(), List.of()));

        mockMvc.perform(post("/api/textbooks/imports/values")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"spreadsheetId": "https://docs.google.com/spreadsheets/d/1AbC/edit", "sheetName": "서책목록",
                                 "values": [["서책코드", "서책명"], ["", ""], [" TB-001 ", null]]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/import-jobs/7"))
                .andExpect(jsonPath("$.createdCount").value(1));

        ArgumentCaptor<SheetSource> source = ArgumentCaptor.forClass(SheetSource.class);
        ArgumentCaptor<SheetTable> table = ArgumentCaptor.forClass(SheetTable.class);
        then(importService).should().importTable(source.capture(), table.capture());
        assertThat(source.getValue()).isEqualTo(new GoogleSheetSource("1AbC", "서책목록"));
        assertThat(table.getValue().headers()).containsExactly("서책코드", "서책명");
        // 빈 행(2행)은 빠지고, 행 번호는 시트의 행 번호 그대로다. null 셀은 빈 문자열이 된다.
        assertThat(table.getValue().rows()).singleElement().satisfies(row -> {
            assertThat(row.rowNumber()).isEqualTo(3);
            assertThat(row.get("서책코드")).isEqualTo("TB-001");
            assertThat(row.get("서책명")).isEmpty();
        });
    }

    @Test
    void values가_비어_있으면_400_ProblemDetail로_거부한다() throws Exception {
        mockMvc.perform(post("/api/textbooks/imports/values")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"spreadsheetId": "1AbC", "values": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        then(importService).shouldHaveNoInteractions();
    }
}
