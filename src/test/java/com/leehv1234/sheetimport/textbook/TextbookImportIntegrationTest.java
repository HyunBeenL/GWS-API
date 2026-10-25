package com.leehv1234.sheetimport.textbook;

import com.leehv1234.sheetimport.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 MySQL 8.4(Testcontainers)에 Flyway 마이그레이션을 적용하고 서책 저장 흐름을 검증한다.
 * Docker가 없으면 이 클래스는 건너뛴다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class TextbookImportIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TextbookRepository textbookRepository;

    @Test
    void 같은_시트를_다시_보내면_바뀐_행만_수정하고_시트에_없는_컬럼은_그대로_둔다() throws Exception {
        mockMvc.perform(importValues("""
                        [["서책코드", "서책명", "저자", "사용여부"],
                         ["IT-TB-001", "국어 3-1", "홍길동", "Y"],
                         ["IT-TB-002", "수학 3-1", "", "n"],
                         ["IT-TB-003", "", "", "예"]]
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PARTIAL_SUCCESS"))
                .andExpect(jsonPath("$.createdCount").value(2))
                .andExpect(jsonPath("$.failedCount").value(1))
                .andExpect(jsonPath("$.errors[0].rowNumber").value(4));

        // 두 번째 시트에는 저자 컬럼이 없다. 저자는 그대로 두고 사용여부만 바뀌어야 한다.
        String location = mockMvc.perform(importValues("""
                        [["서책코드", "서책명", "사용여부"],
                         ["IT-TB-001", "국어 3-1", "N"],
                         ["IT-TB-002", "수학 3-1", "N"]]
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.createdCount").value(0))
                .andExpect(jsonPath("$.updatedCount").value(1))
                .andExpect(jsonPath("$.unchangedCount").value(1))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("GOOGLE_SHEETS"))
                .andExpect(jsonPath("$.sourceRef").value("1AbC [서책목록]"));

        assertThat(textbookRepository.findAllByCodeInAndDeletedAtIsNull(List.of("IT-TB-001")))
                .singleElement()
                .satisfies(textbook -> {
                    assertThat(textbook.getUseYn()).isEqualTo("N");
                    assertThat(textbook.getAuthor()).isEqualTo("홍길동");
                    assertThat(textbook.getCreateDate()).isNotNull();
                });
    }

    private static MockHttpServletRequestBuilder importValues(String values) {
        return post("/api/textbooks/imports/values")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"spreadsheetId\": \"1AbC\", \"sheetName\": \"서책목록\", \"values\": " + values + "}");
    }
}
