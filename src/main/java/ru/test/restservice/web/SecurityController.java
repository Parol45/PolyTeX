package ru.test.restservice.web;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
@Api(description="Контроллер отвечающий за аутентификацию в приложении")
class SecurityController {

    @GetMapping("/login")
    @ApiOperation("Возврат страницы входа в систему")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/banned")
    @ApiOperation("Возврат страницы входа в систему + уведомление о блокировке учётной записи СПбПУ")
    public ModelAndView bannedPage() {
        ModelAndView login = new ModelAndView("login");
        login.addObject("banned", "Вы заблокированы.");
        return login;
    }

    @GetMapping("/cred-error")
    @ApiOperation("Возврат страницы входа в систему + сообщение об ошибке авторизации (блокировка или неверные данные)")
    public ModelAndView badCredPage() {
        ModelAndView login = new ModelAndView("login");
        login.addObject("error", "Неверные данные или Вы заблокированы.");
        return login;
    }

}