package com.leehv1234.sheetimport.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("Sheet Import API")
                .description("Google Sheets / Excel 데이터를 MySQL로 가져오는 API")
                .version("v1"));
    }
}
