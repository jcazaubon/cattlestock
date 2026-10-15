package com.kinetix.cattlestock;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Prueba de cableado: JTE renderiza la pantalla y HTMX consume fragmentos.
 * Reemplazar por las pantallas reales (alta de movimientos, planilla).
 */
@Controller
public class HomeController {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("appName", "CattleStock");
        return "index";
    }

    @GetMapping("/fragment/ping")
    public String ping(Model model) {
        model.addAttribute("now", LocalDateTime.now().format(FECHA_HORA));
        return "fragment/ping";
    }
}
