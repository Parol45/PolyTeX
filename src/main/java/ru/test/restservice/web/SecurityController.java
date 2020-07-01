package ru.test.restservice.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
class SecurityController {

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/banned")
    public ModelAndView bannedPage() {
        ModelAndView login = new ModelAndView("login");
        login.addObject("banned", "Вы заблокированы.");
        return login;
    }

    @GetMapping("/cred-error")
    public ModelAndView badCredPage() {
        ModelAndView login = new ModelAndView("login");
        login.addObject("error", "Неверные данные или Вы заблокированы.");
        return login;
    }

}