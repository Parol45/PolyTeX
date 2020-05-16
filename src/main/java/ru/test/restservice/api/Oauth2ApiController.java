package ru.test.restservice.api;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.test.restservice.service.OAuth2Service;

@RestController
@RequiredArgsConstructor
public class Oauth2ApiController {

    private final OAuth2Service oAuth2Service;

    @GetMapping("/callback")
    public String accessCodeProcessing(@RequestParam String code, @RequestParam String state) {
        return oAuth2Service.getUserInfo(code);
    }

    @GetMapping("/user")
    public Object user(Authentication auth) {
        return auth;
    }
}
