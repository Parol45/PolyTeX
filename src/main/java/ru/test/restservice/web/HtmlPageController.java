package ru.test.restservice.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

//Возврат клиенту пока что единственной существующей страницы
@Controller
public class HtmlPageController {
    @GetMapping("/")
    public String getTestPage() {
        return "index.html";
    }
}