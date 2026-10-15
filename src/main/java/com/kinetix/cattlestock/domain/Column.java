package com.kinetix.cattlestock.domain;

/** Columnas de la planilla mensual. Un movimiento genera una o dos columnas. */
public enum Column {
    COMPRA(true), NACIMIENTO(true), TRASLADO_ENTRA(true), CAMBIO_ENTRA(true),
    MORTANDAD(false), CONSUMO(false), VENTA(false), TRASLADO_SALE(false), CAMBIO_SALE(false);

    public final boolean entrada;

    Column(boolean entrada) {
        this.entrada = entrada;
    }
}
