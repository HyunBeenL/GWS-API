package com.leehv1234.sheetimport;

import org.springframework.boot.SpringApplication;

/**
 * 로컬에 MySQL이 없어도 Testcontainers로 MySQL 8.4를 띄워 앱을 실행한다. (Docker 필요)
 * 실행: ./gradlew bootTestRun
 */
public class TestSheetImportApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(SheetImportApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
