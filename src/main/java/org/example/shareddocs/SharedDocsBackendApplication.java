package org.example.shareddocs;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("org.example.shareddocs.mapper")
@EnableScheduling
public class SharedDocsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SharedDocsBackendApplication.class, args);
    }

}
