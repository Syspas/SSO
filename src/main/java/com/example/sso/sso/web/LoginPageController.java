package com.example.sso.sso.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Страница входа SSO (Thymeleaf).
 *
 * <p>Обработку POST /login делает Spring Security, не этот контроллер.
 */
@Controller
public class LoginPageController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
