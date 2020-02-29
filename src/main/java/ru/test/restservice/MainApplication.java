package ru.test.restservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//До сих пор без понятия, что тут происходит
@SpringBootApplication
public class MainApplication {
    public static boolean isWindows;

    public static void main(String[] args) {
        isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        SpringApplication.run(MainApplication.class, args);
    }


}