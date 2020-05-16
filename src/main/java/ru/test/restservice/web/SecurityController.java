package ru.test.restservice.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.test.restservice.dao.UserRepository;
import ru.test.restservice.entity.User;
import ru.test.restservice.service.LogService;

@Controller
@RequiredArgsConstructor
class SecurityController {

    private final UserRepository userRepository;
    private final LogService logService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/registration")
    public String registrationPage() {
        return "registration";
    }

    // TODO: обрабатывать нарушение уник индекса
    @PostMapping("/registration")
    public RedirectView register(@RequestParam("email") String email,
                                 @RequestParam("password") String password) {
        User newUser = new User(email, new BCryptPasswordEncoder(4).encode(password));
        userRepository.save(newUser);
        logService.log(email, String.format("User %s has registrated", newUser.email));
        return new RedirectView("/projects");
    }

    @GetMapping("/cred-error")
    public ModelAndView badCredPage() {
        ModelAndView login = new ModelAndView("login");
        login.addObject("error", "Wrong credentials");
        return login;
    }

}