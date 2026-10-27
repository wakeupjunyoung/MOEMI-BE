package com.example.moemi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MoemiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoemiApplication.class, args);
    }

}
