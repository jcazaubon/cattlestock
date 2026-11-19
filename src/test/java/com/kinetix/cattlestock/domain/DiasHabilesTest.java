package com.kinetix.cattlestock.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

/** El ultimo dia habil de un mes: de lunes a viernes, sin feriados. */
class DiasHabilesTest {

    @Test
    void siElMesTerminaEnDiaDeSemanaEsElUltimoDia() {
        assertEquals(LocalDate.of(2026, 11, 30), DiasHabiles.ultimoDiaHabil(YearMonth.of(2026, 11))); // lunes
        assertEquals(LocalDate.of(2026, 9, 30), DiasHabiles.ultimoDiaHabil(YearMonth.of(2026, 9)));   // miercoles
        assertEquals(LocalDate.of(2028, 2, 29), DiasHabiles.ultimoDiaHabil(YearMonth.of(2028, 2)));   // martes, bisiesto
    }

    @Test
    void siTerminaEnSabadoRetrocedeAlViernes() {
        assertEquals(LocalDate.of(2026, 10, 30), DiasHabiles.ultimoDiaHabil(YearMonth.of(2026, 10))); // el 31 es sabado
    }

    @Test
    void siTerminaEnDomingoRetrocedeAlViernes() {
        assertEquals(LocalDate.of(2027, 1, 29), DiasHabiles.ultimoDiaHabil(YearMonth.of(2027, 1)));   // el 31 es domingo
    }

    @Test
    void paraCualquierMesDeVariosAniosEsDeLunesAViernesYDelMismoMes() {
        for (int anio = 2024; anio <= 2030; anio++) {
            for (int mes = 1; mes <= 12; mes++) {
                YearMonth ym = YearMonth.of(anio, mes);
                LocalDate dia = DiasHabiles.ultimoDiaHabil(ym);
                assertEquals(ym, YearMonth.from(dia));
                assertFalse(dia.getDayOfWeek() == DayOfWeek.SATURDAY || dia.getDayOfWeek() == DayOfWeek.SUNDAY, dia.toString());
                // y es el ultimo: el siguiente dia del mes, si lo hay, es de fin de semana
                for (LocalDate d = dia.plusDays(1); !d.isAfter(ym.atEndOfMonth()); d = d.plusDays(1)) {
                    assertEquals(true, d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY);
                }
            }
        }
    }
}
