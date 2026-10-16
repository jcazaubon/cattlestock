package com.kinetix.cattlestock.web;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Pagina de error propia, en el estilo de la app, para todo el sitio.
 * Reemplaza la respuesta cruda de Spring (400, 403, 404, 405, 500).
 */
@Controller
public class AppErrorController implements ErrorController {

    @GetMapping("/error")
    public String error(Model model, HttpServletRequest request) {
        Object estado = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int codigo = estado instanceof Integer i ? i : 500;
        model.addAttribute("codigo", codigo);
        model.addAttribute("titulo", switch (codigo) {
            case 400 -> "Envio invalido";
            case 403 -> "Acceso restringido";
            case 404 -> "No existe";
            case 405 -> "Accion no permitida";
            default -> "Algo se rompio";
        });
        model.addAttribute("detalle", switch (codigo) {
            case 400 -> "Falto completar un campo o algun dato vino mal. Volve al formulario y revisa.";
            case 403 -> "No tenes permiso para eso, o la sesion expiro. Inicia sesion de nuevo.";
            case 404 -> "Esa direccion no existe o se movio de lugar.";
            case 405 -> "Esa direccion no acepta ese metodo: por ejemplo, entrar con GET a algo que espera POST.";
            default -> "Ocurrio un error inesperado. Quedo registrado en la consola del servidor.";
        });
        return "error";
    }
}
