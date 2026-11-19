package com.kinetix.cattlestock.web;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kinetix.cattlestock.domain.DiasHabiles;

import jakarta.servlet.http.HttpServletResponse;

/** El campo de fecha de las pantallas de carga, que se recalcula al cambiar el periodo (mes y anio). */
@Controller
public class FechaController {

    /**
     * Devuelve el campo de fecha con el ultimo dia habil del periodo elegido. Avisa con un evento que la fecha
     * cambio, para que el resto de la pantalla (por ejemplo, los animales disponibles a esa fecha) se actualice.
     */
    @GetMapping("/campo-fecha")
    public String campoFecha(@RequestParam(required = false) String periodo, Model model,
                             HttpServletResponse response) {
        model.addAttribute("fecha", DiasHabiles.porDefecto(periodoDe(periodo), java.time.LocalDate.now()).toString());
        response.setHeader("HX-Trigger", "fecha-actualizada");
        return "fragment/campoFecha";
    }

    /**
     * El periodo de una pantalla de carga: el mes de la fecha si ya hay una (por ejemplo, al volver de un error con lo
     * escrito), si no el periodo pedido y, si tampoco, el mes actual.
     */
    static YearMonth periodoDe(String fecha, String periodo) {
        try {
            if (fecha != null && !fecha.isBlank()) return YearMonth.from(java.time.LocalDate.parse(fecha));
        } catch (DateTimeParseException e) {
            // una fecha invalida se informa al guardar; aca se cae al periodo
        }
        return periodoDe(periodo);
    }

    /** El periodo pedido, o el mes actual si falta o no es un mes valido. */
    static YearMonth periodoDe(String periodo) {
        try {
            return periodo == null || periodo.isBlank() ? YearMonth.now() : YearMonth.parse(periodo);
        } catch (DateTimeParseException e) {
            return YearMonth.now();
        }
    }
}
