package com.aces.tennosquad;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TennoSquadApplication {

    public static void main(String[] args) {
        SpringApplication.run(TennoSquadApplication.class, args);
        System.out.println("http://localhost:8093/swagger-ui/index.html");
        System.out.println("http://localhost:8093/scalar");
    }
}
