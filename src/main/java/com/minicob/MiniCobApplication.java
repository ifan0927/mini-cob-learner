package com.minicob;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MiniCobApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniCobApplication.class, args);
    }
}
