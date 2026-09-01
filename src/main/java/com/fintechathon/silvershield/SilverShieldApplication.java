package com.fintechathon.silvershield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SilverShieldApplication {

    public static void main(String[] args) {
        SpringApplication.run(SilverShieldApplication.class, args);
    }
}
