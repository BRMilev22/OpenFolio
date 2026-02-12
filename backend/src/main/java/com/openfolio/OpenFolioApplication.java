package com.openfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OpenFolioApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenFolioApplication.class, args);
    }
}
