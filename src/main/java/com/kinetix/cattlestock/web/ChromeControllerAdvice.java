package com.kinetix.cattlestock.web;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.kinetix.cattlestock.data.UserEntity;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Expone a todas las vistas lo comun del cromo: el token CSRF (JTE no lo inyecta solo)
 * y el usuario logueado con su rol, para la barra y el menu de usuario.
 */
@ControllerAdvice
public class ChromeControllerAdvice {

    private final CurrentUser currentUser;

    public ChromeControllerAdvice(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @ModelAttribute
    public void csrfToken(Model model, HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token == null) {
            token = (CsrfToken) request.getAttribute("_csrf");
        }
        if (token != null) {
            model.addAttribute("_csrf", token);
        }
    }

    @ModelAttribute("usuario")
    public String usuario() {
        return currentUser.get().map(UserEntity::getUsername).orElse("");
    }

    @ModelAttribute("esAdmin")
    public boolean esAdmin() {
        return currentUser.esAdmin();
    }
}
