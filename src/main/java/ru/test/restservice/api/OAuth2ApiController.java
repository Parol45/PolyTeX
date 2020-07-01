package ru.test.restservice.api;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import ru.test.restservice.service.OAuth2Service;

@RestController
@RequiredArgsConstructor
public class OAuth2ApiController {

    private final OAuth2Service oAuth2Service;

    @GetMapping("/callback")
    public RedirectView accessCodeProcessing(@RequestParam String code, @RequestParam String state) {
        if (oAuth2Service.getUserInfo(code)) {
            return new RedirectView("/projects");
        } else {
            return new RedirectView("/banned");
        }
    }

    @GetMapping("/user")
    public Object user(Authentication auth) {
        return auth;
    }

}
