package com.kinetix.cattlestock.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kinetix.cattlestock.app.UserService;
import com.kinetix.cattlestock.data.UserEntity;
import com.kinetix.cattlestock.domain.DomainException;

/** Cada usuario cambia su propia contraseña. Al primer ingreso con la clave del administrador es obligatorio. */
@Controller
public class CuentaController {

    private final UserService userService;
    private final CurrentUser currentUser;

    public CuentaController(UserService userService, CurrentUser currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;
    }

    @GetMapping("/cuenta/clave")
    public String formulario(Model model) {
        return vista(model, null);
    }

    @PostMapping("/cuenta/clave")
    public String cambiar(@RequestParam String actual, @RequestParam String nueva, @RequestParam String repetir,
                          Model model) {
        String usuario = currentUser.get().map(UserEntity::getUsername).orElse("");
        try {
            if (!nueva.equals(repetir)) throw new DomainException("Las contraseñas nuevas no coinciden");
            userService.cambiarClave(usuario, actual, nueva);
        } catch (DomainException e) {
            return vista(model, e.getMessage());
        }
        return "redirect:/";
    }

    private String vista(Model model, String error) {
        model.addAttribute("error", error);
        model.addAttribute("forzada", currentUser.get().map(UserEntity::isMustChangePassword).orElse(false));
        return "clave";
    }
}
