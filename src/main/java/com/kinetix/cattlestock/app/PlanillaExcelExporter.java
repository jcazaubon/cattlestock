package com.kinetix.cattlestock.app;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.kinetix.cattlestock.domain.Column;
import com.kinetix.cattlestock.domain.Planilla;
import com.kinetix.cattlestock.domain.Species;

/** Arma la planilla mensual en .xlsx con el formato de las planillas actuales. */
@Component
public class PlanillaExcelExporter {

    private static final Column[] COLUMNAS = {
            Column.COMPRA, Column.NACIMIENTO, Column.TRASLADO_ENTRA, Column.CAMBIO_ENTRA,
            Column.VENTA, Column.MORTANDAD, Column.CONSUMO, Column.TRASLADO_SALE, Column.CAMBIO_SALE };

    private static final String[] TITULOS = {
            "Compra", "Nacimiento", "Traslado entra", "Cambio entra",
            "Venta", "Mortandad", "Consumo", "Traslado sale", "Cambio sale" };

    public void write(Planilla planilla, OutputStream out) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Planilla");
            CellStyle bold = bold(wb);

            int fila = 0;
            Row titulo = sheet.createRow(fila++);
            celda(titulo, 0, planilla.establishment().name() + " - " + planilla.month()
                    + (planilla.closed() ? " (cerrada)" : ""), bold);

            fila++;
            for (Species especie : Species.values()) {
                List<Planilla.Row> filas = planilla.rows().stream()
                        .filter(r -> r.category().species() == especie).toList();
                if (filas.isEmpty()) continue;
                fila = seccion(sheet, fila, etiqueta(especie), filas, planilla.totalFor(especie), bold);
            }

            Planilla.Totals general = planilla.totalGeneral();
            Row r = sheet.createRow(fila++);
            celda(r, 0, "Total general", bold);
            celda(r, 1, general.anterior(), bold);
            int col = 2;
            for (Column columna : COLUMNAS) celda(r, col++, general.columns().get(columna), bold);
            celda(r, 13, general.finalStock(), bold);

            for (int i = 0; i < 14; i++) sheet.autoSizeColumn(i);
            wb.write(out);
        }
    }

    private int seccion(Sheet sheet, int fila, String etiqueta, List<Planilla.Row> filas,
                       Planilla.Totals totales, CellStyle bold) {
        Row rotulo = sheet.createRow(fila++);
        celda(rotulo, 0, etiqueta, bold);
        Row encabezado = sheet.createRow(fila++);
        celda(encabezado, 0, "Categoria", bold);
        celda(encabezado, 1, "Anterior", bold);
        int col = 2;
        for (String titulo : TITULOS) celda(encabezado, col++, titulo, bold);
        celda(encabezado, col++, "Entradas", bold);
        celda(encabezado, col++, "Salidas", bold);
        celda(encabezado, col++, "Final", bold);

        for (Planilla.Row f : filas) {
            Row r = sheet.createRow(fila++);
            celda(r, 0, f.category().name(), null);
            celda(r, 1, f.anterior(), null);
            int c = 2;
            for (Column columna : COLUMNAS) celda(r, c++, f.columns().get(columna), null);
            celda(r, c++, f.entradas(), null);
            celda(r, c++, f.salidas(), null);
            celda(r, c++, f.finalStock(), bold);
        }

        Row t = sheet.createRow(fila++);
        celda(t, 0, "Total " + etiqueta, bold);
        celda(t, 1, totales.anterior(), bold);
        int c = 2;
        for (Column columna : COLUMNAS) celda(t, c++, totales.columns().get(columna), bold);
        celda(t, 13, totales.finalStock(), bold);
        return fila;
    }

    private String etiqueta(Species especie) {
        return especie == Species.VACUNOS ? "Vacunos"
                : especie == Species.YEGUARIZOS ? "Yeguarizos" : "Lanares";
    }

    private void celda(Row fila, int columna, Object valor, CellStyle estilo) {
        Cell c = fila.createCell(columna);
        if (valor instanceof Integer i) c.setCellValue(i);
        else c.setCellValue(String.valueOf(valor));
        if (estilo != null) c.setCellStyle(estilo);
    }

    private CellStyle bold(Workbook wb) {
        Font fuente = wb.createFont();
        fuente.setBold(true);
        CellStyle estilo = wb.createCellStyle();
        estilo.setFont(fuente);
        return estilo;
    }
}
