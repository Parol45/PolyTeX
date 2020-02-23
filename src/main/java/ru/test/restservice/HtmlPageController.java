package ru.test.restservice;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class HtmlPageController {
    @GetMapping("/")
    public String getTestPage() {
        return "index.html";
    }
}