package com.kinetix.cattlestock.domain;

import java.time.LocalDate;

/** Efecto de un movimiento sobre una categoria de un establecimiento. */
public record Effect(String establishmentId, String categoryId, LocalDate date, Column column, int quantity) {
    public int signed() {
        return column.entrada ? quantity : -quantity;
    }

    String key() {
        return Ledger.key(establishmentId, categoryId);
    }
}
