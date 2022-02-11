package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@SpringBootApplication
@RestController
public class DemoApplication {


    @Value("${test1.example}")
    private String string;

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }


    @GetMapping("/")
    public String hello(){
        return new String(Base64.getDecoder().decode(string), StandardCharsets.UTF_8);
    }

}
