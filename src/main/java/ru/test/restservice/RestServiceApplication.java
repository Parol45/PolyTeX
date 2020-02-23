package ru.test.restservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RestServiceApplication {
    public static boolean isWindows;
    public static void main(String[] args) {
        isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        SpringApplication.run(RestServiceApplication.class, args);
    }

}