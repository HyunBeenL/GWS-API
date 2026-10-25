package com.leehv1234.sheetimport.spreadsheet.google;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * {@code google.sheets.credentials}가 있을 때만 Google Sheets 클라이언트를 만든다.
 * 없으면 이 설정 전체가 빠지고, 엑셀 업로드만으로도 앱이 정상 기동한다.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "google.sheets", name = "credentials")
public class GoogleSheetsConfig {

    @Bean
    Sheets sheets(GoogleSheetsProperties properties) throws IOException, GeneralSecurityException {
        ServiceAccountCredentials credentials;
        try (InputStream in = properties.credentials().getInputStream()) {
            credentials = ServiceAccountCredentials.fromStream(in);
        }
        // 서비스 계정은 "사람이 아닌 별도의 구글 계정"이다. 이 이메일에 시트를 공유해야 읽을 수 있다.
        log.info("Google Sheets 연동 활성화 - 읽을 시트를 이 계정에 공유하세요: {}", credentials.getClientEmail());

        HttpCredentialsAdapter auth = new HttpCredentialsAdapter(
                credentials.createScoped(List.of(SheetsScopes.SPREADSHEETS_READONLY)));
        HttpRequestInitializer initializer = request -> {
            auth.initialize(request);
            request.setConnectTimeout((int) properties.connectTimeout().toMillis());
            request.setReadTimeout((int) properties.readTimeout().toMillis());
        };

        return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), initializer)
                .setApplicationName(properties.applicationName())
                .build();
    }

    @Bean
    GoogleSheetReader googleSheetReader(Sheets sheets) {
        return new GoogleSheetReader(sheets);
    }
}
