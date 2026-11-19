package com.kinetix.cattlestock.domain;

public enum MovementType {
    COMPRA, VENTA, NACIMIENTO, MORTANDAD, CONSUMO, TRASLADO, CAMBIO_CATEGORIA,
    /** Animales que salen a pastoreo en campo de terceros (columna "Pastoreo" de las planillas). */
    PASTOREO,
    /** Diferencia de recuento a favor: el conteo fisico encontro mas animales que la planilla. */
    RECUENTO_MAS,
    /** Diferencia de recuento en contra: el conteo fisico encontro menos animales que la planilla. */
    RECUENTO_MENOS
}
