package com.kinetix.cattlestock.web;

import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Ayudas del selector de periodo (mes y año), el mismo en todas las pantallas; por defecto el mes actual. */
public final class PeriodoVista {

    private PeriodoVista() {
    }

    /** El periodo aaaa-mm, o el mes actual si falta o no es valido. */
    public static YearMonth analizar(String valor) {
        try {
            return valor == null || valor.isBlank() ? YearMonth.now() : YearMonth.parse(valor);
        } catch (java.time.format.DateTimeParseException e) {
            return YearMonth.now();
        }
    }

    public static String nombreMes(int mes) {
        String n = Month.of(mes).getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"));
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    /** Los años cercanos (cinco atras, uno adelante), mas el elegido si queda fuera; el mas nuevo primero. */
    public static List<Integer> anios(YearMonth elegido) {
        int hoy = Year.now().getValue();
        int desde = Math.min(hoy - 5, elegido.getYear());
        int hasta = Math.max(hoy + 1, elegido.getYear());
        List<Integer> out = new ArrayList<>();
        for (int a = hasta; a >= desde; a--) out.add(a);
        return out;
    }
}
