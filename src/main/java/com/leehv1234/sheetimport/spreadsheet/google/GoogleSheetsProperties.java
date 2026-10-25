package com.leehv1234.sheetimport.spreadsheet.google;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.Resource;

import java.time.Duration;

/**
 * @param credentials     서비스 계정 JSON 키 위치 (예: {@code file:./secrets/service-account.json})
 * @param applicationName Google API 요청에 실리는 애플리케이션 이름
 */
@ConfigurationProperties("google.sheets")
public record GoogleSheetsProperties(
        Resource credentials,
        @DefaultValue("sheet-import-api") String applicationName,
        @DefaultValue("10s") Duration connectTimeout,
        @DefaultValue("30s") Duration readTimeout) {
}
