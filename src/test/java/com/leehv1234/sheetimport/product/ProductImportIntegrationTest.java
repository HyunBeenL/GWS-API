package com.leehv1234.sheetimport.product;

import com.leehv1234.sheetimport.TestcontainersConfiguration;
import com.leehv1234.sheetimport.support.ExcelFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 MySQL 8.4(Testcontainers)에 Flyway 마이그레이션을 적용하고 전체 흐름을 검증한다.
 * Docker가 없으면 이 클래스는 건너뛴다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ProductImportIntegrationTest {

    private static final Object[] HEADER = {"상품코드", "상품명", "카테고리", "가격", "재고"};

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ProductRepository productRepository;

    @Test
    void 같은_엑셀을_다시_올리면_바뀐_행만_수정된다() throws Exception {
        byte[] first = ExcelFixture.xlsx("상품목록", HEADER,
                new Object[]{"IT-001", "연필", "문구", 1000, 10},
                new Object[]{"IT-002", "지우개", "문구", 500, 20},
                new Object[]{"IT-003", "", "문구", "abc", 1});

        mockMvc.perform(multipart("/api/products/imports/excel").file(excel(first)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PARTIAL_SUCCESS"))
                .andExpect(jsonPath("$.createdCount").value(2))
                .andExpect(jsonPath("$.failedCount").value(1))
                .andExpect(jsonPath("$.errors[0].rowNumber").value(4));

        byte[] second = ExcelFixture.xlsx("상품목록", HEADER,
                new Object[]{"IT-001", "연필", "문구", 1200, 10},
                new Object[]{"IT-002", "지우개", "문구", 500, 20});

        String location = mockMvc.perform(multipart("/api/products/imports/excel").file(excel(second)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.createdCount").value(0))
                .andExpect(jsonPath("$.updatedCount").value(1))
                .andExpect(jsonPath("$.unchangedCount").value(1))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("EXCEL"))
                .andExpect(jsonPath("$.errors").isEmpty());

        assertThat(productRepository.findAllBySkuIn(List.of("IT-001")))
                .singleElement()
                .satisfies(product -> assertThat(product.getPrice()).isEqualByComparingTo("1200"));
    }

    private static MockMultipartFile excel(byte[] content) {
        return new MockMultipartFile("file", "products.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);
    }
}
