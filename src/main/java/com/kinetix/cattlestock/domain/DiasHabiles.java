package com.kinetix.cattlestock.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Fechas por defecto para cargar movimientos: el periodo es un mes y la carga se asienta, por defecto, el ultimo
 * dia habil del mes. Habil quiere decir de lunes a viernes: los feriados varian por pais, por provincia y por anio
 * y no se calculan, asi que la fecha siempre se puede cambiar a mano.
 */
public final class DiasHabiles {

    private DiasHabiles() {
    }

    /**
     * La fecha con la que arranca una carga: el ultimo dia habil del periodo, salvo que ese dia todavia no llego
     * (el mes en curso), porque los movimientos se cargan cuando ocurren y no tiene sentido asentarlos a futuro:
     * entonces es hoy. Se puede cambiar a mano.
     */
    public static LocalDate porDefecto(YearMonth mes, LocalDate hoy) {
        LocalDate ultimo = ultimoDiaHabil(mes);
        return hoy.isBefore(ultimo) && YearMonth.from(hoy).equals(mes) ? hoy : ultimo;
    }

    /** El ultimo dia del mes que no es sabado ni domingo. */
    public static LocalDate ultimoDiaHabil(YearMonth mes) {
        LocalDate dia = mes.atEndOfMonth();
        while (dia.getDayOfWeek() == DayOfWeek.SATURDAY || dia.getDayOfWeek() == DayOfWeek.SUNDAY) {
            dia = dia.minusDays(1);
        }
        return dia;
    }
}
