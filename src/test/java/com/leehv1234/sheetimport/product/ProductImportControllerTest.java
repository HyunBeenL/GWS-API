package com.leehv1234.sheetimport.product;

import com.leehv1234.sheetimport.global.error.BusinessException;
import com.leehv1234.sheetimport.global.error.ErrorCode;
import com.leehv1234.sheetimport.importjob.ImportJobResponse;
import com.leehv1234.sheetimport.importjob.ImportStatus;
import com.leehv1234.sheetimport.spreadsheet.SheetSourceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 웹 계층만 띄워 요청/응답 형식과 예외 → ProblemDetail 변환을 검증한다. 서비스는 가짜(mock)다. */
@WebMvcTest(ProductImportController.class)
class ProductImportControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProductImportService importService;

    @Test
    void 엑셀_업로드에_성공하면_201과_이력_조회_주소를_돌려준다() throws Exception {
        given(importService.importFrom(any())).willReturn(response(7L));

        mockMvc.perform(multipart("/api/products/imports/excel").file(excel(new byte[]{1})))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/import-jobs/7"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.createdCount").value(1));
    }

    @Test
    void 빈_파일은_400_ProblemDetail로_거부한다() throws Exception {
        mockMvc.perform(multipart("/api/products/imports/excel").file(excel(new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EMPTY_FILE"));
    }

    @Test
    void 스프레드시트_ID가_없으면_400() throws Exception {
        mockMvc.perform(post("/api/products/imports/google-sheets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"range": "상품목록!A1:E"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 서비스_예외는_ErrorCode의_상태코드와_코드로_응답한다() throws Exception {
        given(importService.importFrom(any())).willThrow(new BusinessException(ErrorCode.GOOGLE_SHEETS_NOT_CONFIGURED));

        mockMvc.perform(post("/api/products/imports/google-sheets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"spreadsheetId": "1AbC"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("GOOGLE_SHEETS_NOT_CONFIGURED"))
                .andExpect(jsonPath("$.detail").value(ErrorCode.GOOGLE_SHEETS_NOT_CONFIGURED.getMessage()));
    }

    private static MockMultipartFile excel(byte[] content) {
        return new MockMultipartFile("file", "products.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);
    }

    private static ImportJobResponse response(Long id) {
        return new ImportJobResponse(id, SheetSourceType.EXCEL, "products.xlsx", ImportStatus.SUCCESS,
                1, 1, 0, 0, 0, LocalDateTime.now(), List.of());
    }
}
