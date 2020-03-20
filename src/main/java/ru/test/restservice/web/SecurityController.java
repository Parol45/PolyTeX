package ru.test.restservice.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import ru.test.restservice.dao.UserRepository;
import ru.test.restservice.entity.User;

import javax.transaction.Transactional;

@Controller
@RequiredArgsConstructor
class SecurityController {

    private final UserRepository userRepository;

    @GetMapping("/login")
    public String loginPage() {
        return "login.html";
    }

    @GetMapping("/registration")
    public String registerPage() {
        return "registration.html";
    }

    @PostMapping("/registration")
    @Transactional
    public RedirectView register(@RequestParam("email") String email,
                                 @RequestParam("password") String password) {
        userRepository.save(new User(email, new BCryptPasswordEncoder(4).encode(password)));
        return new RedirectView("/login");
    }

}