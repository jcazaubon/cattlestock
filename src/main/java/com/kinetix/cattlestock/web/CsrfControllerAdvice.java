package com.kinetix.cattlestock.web;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Deja el token CSRF a disposicion de los templates JTE, que no lo inyectan solos.
 * Cada formulario lo incluye como campo oculto.
 */
@ControllerAdvice
public class CsrfControllerAdvice {

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
}
