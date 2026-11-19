package com.kinetix.cattlestock.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(Model model,
                         @RequestParam(required = false) String error,
                         @RequestParam(required = false) String logout,
                         @RequestParam(required = false) String bloqueado) {
        model.addAttribute("error", bloqueado != null
                ? "Demasiados intentos fallidos. Esperá 15 minutos antes de volver a intentar."
                : error != null ? "Usuario o contraseña incorrectos." : null);
        model.addAttribute("salio", logout != null);
        return "login";
    }
}
