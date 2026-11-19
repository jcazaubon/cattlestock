package com.kinetix.cattlestock.web;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Lo que comparten las dos formas de cargar movimientos (por línea y por planilla). */
final class CargaComun {

    private CargaComun() {
    }

    static LocalDate parseFecha(String fecha) {
        try {
            return LocalDate.parse(fecha);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha no es válida");
        }
    }

    static String texto(String valor, String mensajeSiFalta) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(mensajeSiFalta);
        return valor.trim();
    }
}
