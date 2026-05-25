package com.fiap.vinshare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VinShareApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(VinShareApiApplication.class, args);
    }
}
