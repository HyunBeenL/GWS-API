package com.leehv1234.sheetimport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SheetImportApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SheetImportApiApplication.class, args);
    }
}
