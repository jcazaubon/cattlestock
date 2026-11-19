package com.kinetix.cattlestock.web;

import com.kinetix.cattlestock.domain.Column;
import com.kinetix.cattlestock.domain.Planilla;
import com.kinetix.cattlestock.domain.Species;

/**
 * Ayudas de la pantalla de planilla. En el celular se ocultan las filas y las columnas sin movimiento; aca se
 * calcula cuales son, y la pantalla solo las marca con una clase (en la computadora se ven todas).
 */
public final class PlanillaVista {

    /** El orden de las columnas de movimiento en la pantalla; despues van "Entradas" y "Salidas". */
    static final Column[] COLUMNAS = {
            Column.COMPRA, Column.NACIMIENTO, Column.TRASLADO_ENTRA, Column.CAMBIO_ENTRA, Column.RECUENTO_MAS,
            Column.VENTA, Column.MORTANDAD, Column.CONSUMO, Column.PASTOREO, Column.TRASLADO_SALE,
            Column.CAMBIO_SALE, Column.RECUENTO_MENOS };

    private PlanillaVista() {
    }

    /**
     * Para cada columna de movimiento de la especie (en el orden de {@link #COLUMNAS}) y luego "Entradas" y
     * "Salidas": true si ninguna categoria tuvo movimiento ahi en el mes.
     */
    public static boolean[] columnasVacias(Planilla planilla, Species especie) {
        boolean[] vacias = new boolean[COLUMNAS.length + 2];
        for (int i = 0; i < COLUMNAS.length; i++) {
            final Column columna = COLUMNAS[i];
            vacias[i] = planilla.rows().stream().filter(r -> r.category().species() == especie)
                    .allMatch(r -> r.columns().get(columna) == 0);
        }
        vacias[COLUMNAS.length] = planilla.rows().stream().filter(r -> r.category().species() == especie)
                .allMatch(r -> r.entradas() == 0);
        vacias[COLUMNAS.length + 1] = planilla.rows().stream().filter(r -> r.category().species() == especie)
                .allMatch(r -> r.salidas() == 0);
        return vacias;
    }

    /** Las categorias sin ningun movimiento en el mes, de todas las especies. */
    public static long filasSinMovimiento(Planilla planilla) {
        return planilla.rows().stream().filter(r -> r.entradas() == 0 && r.salidas() == 0).count();
    }
}
