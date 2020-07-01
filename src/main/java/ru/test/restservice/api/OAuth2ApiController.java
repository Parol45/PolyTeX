package ru.test.restservice.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import ru.test.restservice.service.OAuth2Service;

@RestController
@RequiredArgsConstructor
@Api(description="API действий с файлами проекта")
public class OAuth2ApiController {

    private final OAuth2Service oAuth2Service;

    @GetMapping("/callback")
    @ApiOperation("Эндпоинт, завершающий аутентификацию через CAS СПбПУ (https://apereo.github.io/cas/6.2.x/installation/OAuth-OpenId-Authentication.html#authorization-code)")
    public RedirectView accessCodeProcessing(@RequestParam String code, @RequestParam String state) {
        if (oAuth2Service.getUserInfo(code)) {
            return new RedirectView("/projects");
        } else {
            return new RedirectView("/banned");
        }
    }

    @GetMapping("/user")
    @ApiOperation("Получение объекта с информацией о текущей аутентификации (потом переделать в личную страницу пользователя)")
    public Object user(Authentication auth) {
        return auth;
    }

}
