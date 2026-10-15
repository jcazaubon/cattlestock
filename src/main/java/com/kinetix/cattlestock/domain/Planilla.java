package com.kinetix.cattlestock.domain;

import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Formulario mensual de un establecimiento. Se calcula a partir de los movimientos. */
public record Planilla(Establishment establishment, YearMonth month, boolean closed, List<Row> rows) {

    /** Una fila por categoria: existencia anterior, columnas del mes y existencia final. */
    public record Row(Category category, int anterior, Map<Column, Integer> columns, int finalStock) {
        public int entradas() {
            return sum(true);
        }

        public int salidas() {
            return sum(false);
        }

        private int sum(boolean entrada) {
            int total = 0;
            for (Map.Entry<Column, Integer> e : columns.entrySet()) {
                if (e.getKey().entrada == entrada) total += e.getValue();
            }
            return total;
        }
    }

    public record Totals(int anterior, Map<Column, Integer> columns, int finalStock) {
    }

    public Totals totalFor(Species species) {
        return total(rows.stream().filter(r -> r.category().species() == species).toList());
    }

    public Totals totalGeneral() {
        return total(rows);
    }

    private static Totals total(List<Row> selected) {
        Map<Column, Integer> columns = new EnumMap<>(Column.class);
        for (Column c : Column.values()) columns.put(c, 0);
        int anterior = 0;
        int finalStock = 0;
        for (Row r : selected) {
            anterior += r.anterior();
            finalStock += r.finalStock();
            r.columns().forEach((c, q) -> columns.merge(c, q, Integer::sum));
        }
        return new Totals(anterior, columns, finalStock);
    }
}
