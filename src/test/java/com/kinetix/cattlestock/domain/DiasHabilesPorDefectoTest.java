package com.kinetix.cattlestock.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

class DiasHabilesPorDefectoTest {

    @Test
    void enElMesEnCursoAntesDelUltimoDiaHabilLaFechaEsHoy() {
        assertEquals(LocalDate.of(2026, 11, 19), DiasHabiles.porDefecto(YearMonth.of(2026, 11), LocalDate.of(2026, 11, 19)));
    }

    @Test
    void unMesPasadoSigueUsandoElUltimoDiaHabil() {
        assertEquals(LocalDate.of(2026, 9, 30), DiasHabiles.porDefecto(YearMonth.of(2026, 9), LocalDate.of(2026, 11, 19)));
    }

    @Test
    void unMesFuturoUsaElUltimoDiaHabilDeEseMes() {
        assertEquals(LocalDate.of(2026, 12, 31), DiasHabiles.porDefecto(YearMonth.of(2026, 12), LocalDate.of(2026, 11, 19)));
    }

    @Test
    void elUltimoDiaHabilDelMesEnCursoYaEsHoyOPasado() {
        assertEquals(LocalDate.of(2026, 11, 30), DiasHabiles.porDefecto(YearMonth.of(2026, 11), LocalDate.of(2026, 11, 30)));
        // sabado 28/11 con ultimo dia habil lunes 30: hoy, no el lunes
        assertEquals(LocalDate.of(2026, 11, 28), DiasHabiles.porDefecto(YearMonth.of(2026, 11), LocalDate.of(2026, 11, 28)));
        // domingo 1/11? el mes en curso arranca con hoy
        assertEquals(LocalDate.of(2026, 11, 1), DiasHabiles.porDefecto(YearMonth.of(2026, 11), LocalDate.of(2026, 11, 1)));
    }
}
