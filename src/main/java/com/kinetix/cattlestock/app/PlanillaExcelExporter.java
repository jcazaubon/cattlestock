package com.kinetix.cattlestock.app;

import java.io.IOException;
import java.io.OutputStream;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.kinetix.cattlestock.domain.Column;
import com.kinetix.cattlestock.domain.Planilla;
import com.kinetix.cattlestock.domain.Species;

/**
 * Arma la planilla mensual en .xlsx con el formato de las planillas actuales y con el aspecto de la app: las
 * entradas en verde, las salidas en rojo y la existencia final resaltada, bordes finos, numeros con formato,
 * encabezados por grupo y la hoja lista para imprimir en horizontal a un ancho de pagina.
 */
@Component
public class PlanillaExcelExporter {

    private static final Column[] COLUMNAS = {
            Column.COMPRA, Column.NACIMIENTO, Column.TRASLADO_ENTRA, Column.CAMBIO_ENTRA, Column.RECUENTO_MAS,
            Column.VENTA, Column.MORTANDAD, Column.CONSUMO, Column.PASTOREO, Column.TRASLADO_SALE,
            Column.CAMBIO_SALE, Column.RECUENTO_MENOS };

    private static final String[] TITULOS = {
            "Compra", "Nacimiento", "Transf. DE", "Cambio DE", "Dif. recuento +",
            "Venta", "Mortandad", "Consumo", "Pastoreo", "Transf. A", "Cambio A", "Dif. recuento -" };

    /** Las primeras columnas de entrada son las 5 primeras de movimiento; las demas son de salida. */
    private static final int ENTRADAS = 5;

    /** Indice de la columna "Final": categoria, anterior, las columnas de movimiento, entradas y salidas. */
    private static final int COL_FINAL = 2 + COLUMNAS.length + 2;

    // paleta de la app
    private static final byte[] TINTA = {0x14, 0x26, 0x2B};
    private static final byte[] GRIS = {(byte) 0xF1, (byte) 0xF4, (byte) 0xEF};
    private static final byte[] BANDA = {(byte) 0xF6, (byte) 0xF8, (byte) 0xF4};
    private static final byte[] VERDE_SUAVE = {(byte) 0xE3, (byte) 0xF0, (byte) 0xE6};
    private static final byte[] ROJO_SUAVE = {(byte) 0xFB, (byte) 0xEA, (byte) 0xE7};
    private static final byte[] AMARILLO_SUAVE = {(byte) 0xFD, (byte) 0xF3, (byte) 0xCC};
    private static final byte[] AMARILLO = {(byte) 0xF2, (byte) 0xB7, 0x05};
    private static final byte[] VERDE = {0x17, 0x51, 0x2F};
    private static final byte[] ROJO = {(byte) 0xB3, 0x34, 0x2A};
    private static final byte[] BORDE = {(byte) 0xB9, (byte) 0xC4, (byte) 0xBB};

    private static final String FORMATO_NUMERO = "#,##0;-#,##0;\"-\"";

    /** Los estilos de una hoja, creados una sola vez: un libro no admite estilos sin limite. */
    private static final class Estilos {
        CellStyle titulo;
        CellStyle subtitulo;
        CellStyle rotuloEspecie;
        CellStyle encabezado;
        CellStyle encabezadoEntrada;
        CellStyle encabezadoSalida;
        CellStyle encabezadoFinal;
        CellStyle texto;
        CellStyle textoBanda;
        CellStyle numero;
        CellStyle numeroBanda;
        CellStyle numeroFinal;
        CellStyle totalTexto;
        CellStyle totalNumero;
        CellStyle totalFinal;
        CellStyle generalTexto;
        CellStyle generalNumero;
        CellStyle generalFinal;
    }

    public void write(Planilla planilla, OutputStream out) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Planilla");
            Estilos e = estilos(wb);

            int fila = 0;
            String mes = planilla.month().getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"))
                    + " " + planilla.month().getYear();
            Row titulo = sheet.createRow(fila++);
            titulo.setHeightInPoints(26);
            celda(titulo, 0, planilla.establishment().name() + " - " + mes
                    + (planilla.closed() ? " (cerrada)" : ""), e.titulo);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, COL_FINAL));

            Row subtitulo = sheet.createRow(fila++);
            celda(subtitulo, 0, "Planilla mensual de hacienda: existencia anterior, movimientos del mes y existencia final",
                    e.subtitulo);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, COL_FINAL));

            fila++;
            for (Species especie : Species.values()) {
                List<Planilla.Row> filas = planilla.rows().stream()
                        .filter(r -> r.category().species() == especie).toList();
                if (filas.isEmpty()) continue;
                fila = seccion(sheet, fila, etiqueta(especie), filas, planilla.totalFor(especie), e);
            }

            Planilla.Totals general = planilla.totalGeneral();
            Row r = sheet.createRow(fila++);
            r.setHeightInPoints(20);
            celda(r, 0, "Total general", e.generalTexto);
            celda(r, 1, general.anterior(), e.generalNumero);
            int col = 2;
            for (Column columna : COLUMNAS) celda(r, col++, general.columns().get(columna), e.generalNumero);
            celda(r, col++, general.columns().entrySet().stream().filter(x -> x.getKey().entrada)
                    .mapToInt(java.util.Map.Entry::getValue).sum(), e.generalNumero);
            celda(r, col++, general.columns().entrySet().stream().filter(x -> !x.getKey().entrada)
                    .mapToInt(java.util.Map.Entry::getValue).sum(), e.generalNumero);
            celda(r, COL_FINAL, general.finalStock(), e.generalFinal);

            sheet.setColumnWidth(0, 34 * 256);
            for (int i = 1; i <= COL_FINAL; i++) sheet.setColumnWidth(i, 12 * 256);
            sheet.createFreezePane(1, 0);
            sheet.setDisplayGridlines(false);

            // impresion: horizontal, a un ancho de pagina y con el numero de pagina
            sheet.setFitToPage(true);
            sheet.getPrintSetup().setLandscape(true);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.getPrintSetup().setFitHeight((short) 0);
            sheet.setHorizontallyCenter(true);
            sheet.getFooter().setCenter("Pagina &P de &N");
            wb.write(out);
        }
    }

    private int seccion(Sheet sheet, int fila, String etiqueta, List<Planilla.Row> filas,
                        Planilla.Totals totales, Estilos e) {
        Row rotulo = sheet.createRow(fila++);
        rotulo.setHeightInPoints(20);
        celda(rotulo, 0, etiqueta, e.rotuloEspecie);
        for (int c = 1; c <= COL_FINAL; c++) rotulo.createCell(c).setCellStyle(e.rotuloEspecie);

        // encabezado de dos filas: el grupo (Entradas / Salidas) y debajo cada columna
        int grupo = fila;
        Row filaGrupo = sheet.createRow(fila++);
        Row encabezado = sheet.createRow(fila++);
        encabezado.setHeightInPoints(30);
        celda(filaGrupo, 0, "Categoría", e.encabezado);
        celda(filaGrupo, 1, "Anterior", e.encabezado);
        celda(encabezado, 0, "", e.encabezado);
        celda(encabezado, 1, "", e.encabezado);
        sheet.addMergedRegion(new CellRangeAddress(grupo, grupo + 1, 0, 0));
        sheet.addMergedRegion(new CellRangeAddress(grupo, grupo + 1, 1, 1));

        celda(filaGrupo, 2, "Entradas", e.encabezadoEntrada);
        for (int c = 3; c < 2 + ENTRADAS; c++) filaGrupo.createCell(c).setCellStyle(e.encabezadoEntrada);
        sheet.addMergedRegion(new CellRangeAddress(grupo, grupo, 2, 1 + ENTRADAS));
        celda(filaGrupo, 2 + ENTRADAS, "Salidas", e.encabezadoSalida);
        for (int c = 3 + ENTRADAS; c < 2 + COLUMNAS.length; c++) filaGrupo.createCell(c).setCellStyle(e.encabezadoSalida);
        sheet.addMergedRegion(new CellRangeAddress(grupo, grupo, 2 + ENTRADAS, 1 + COLUMNAS.length));

        for (int i = 0; i < TITULOS.length; i++) {
            celda(encabezado, 2 + i, TITULOS[i], i < ENTRADAS ? e.encabezadoEntrada : e.encabezadoSalida);
        }
        int col = 2 + COLUMNAS.length;
        celda(filaGrupo, col, "Total entradas", e.encabezadoEntrada);
        celda(encabezado, col, "", e.encabezadoEntrada);
        sheet.addMergedRegion(new CellRangeAddress(grupo, grupo + 1, col, col));
        col++;
        celda(filaGrupo, col, "Total salidas", e.encabezadoSalida);
        celda(encabezado, col, "", e.encabezadoSalida);
        sheet.addMergedRegion(new CellRangeAddress(grupo, grupo + 1, col, col));
        col++;
        celda(filaGrupo, col, "Final", e.encabezadoFinal);
        celda(encabezado, col, "", e.encabezadoFinal);
        sheet.addMergedRegion(new CellRangeAddress(grupo, grupo + 1, col, col));

        int n = 0;
        for (Planilla.Row f : filas) {
            boolean banda = n++ % 2 == 1;
            Row r = sheet.createRow(fila++);
            celda(r, 0, f.category().name(), banda ? e.textoBanda : e.texto);
            CellStyle num = banda ? e.numeroBanda : e.numero;
            celda(r, 1, f.anterior(), num);
            int c = 2;
            for (Column columna : COLUMNAS) celda(r, c++, f.columns().get(columna), num);
            celda(r, c++, f.entradas(), num);
            celda(r, c++, f.salidas(), num);
            celda(r, c++, f.finalStock(), e.numeroFinal);
        }

        Row t = sheet.createRow(fila++);
        t.setHeightInPoints(18);
        celda(t, 0, "Total " + etiqueta, e.totalTexto);
        celda(t, 1, totales.anterior(), e.totalNumero);
        int c = 2;
        for (Column columna : COLUMNAS) celda(t, c++, totales.columns().get(columna), e.totalNumero);
        celda(t, c++, totales.columns().entrySet().stream().filter(x -> x.getKey().entrada)
                .mapToInt(java.util.Map.Entry::getValue).sum(), e.totalNumero);
        celda(t, c++, totales.columns().entrySet().stream().filter(x -> !x.getKey().entrada)
                .mapToInt(java.util.Map.Entry::getValue).sum(), e.totalNumero);
        celda(t, COL_FINAL, totales.finalStock(), e.totalFinal);
        sheet.createRow(fila++); // aire entre secciones
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

    // --- estilos ---

    private Estilos estilos(XSSFWorkbook wb) {
        Estilos e = new Estilos();
        e.titulo = estilo(wb, null, TINTA, 15, true, false, HorizontalAlignment.LEFT, null, false);
        e.subtitulo = estilo(wb, null, new byte[] {0x4B, 0x5D, 0x60}, 10, false, false, HorizontalAlignment.LEFT, null, false);
        e.rotuloEspecie = estilo(wb, GRIS, TINTA, 12, true, false, HorizontalAlignment.LEFT, null, false);
        e.encabezado = estilo(wb, GRIS, TINTA, 10, true, true, HorizontalAlignment.CENTER, null, true);
        e.encabezadoEntrada = estilo(wb, VERDE_SUAVE, VERDE, 10, true, true, HorizontalAlignment.CENTER, null, true);
        e.encabezadoSalida = estilo(wb, ROJO_SUAVE, ROJO, 10, true, true, HorizontalAlignment.CENTER, null, true);
        e.encabezadoFinal = estilo(wb, AMARILLO, TINTA, 10, true, true, HorizontalAlignment.CENTER, null, true);
        e.texto = estilo(wb, null, TINTA, 10, false, true, HorizontalAlignment.LEFT, null, false);
        e.textoBanda = estilo(wb, BANDA, TINTA, 10, false, true, HorizontalAlignment.LEFT, null, false);
        e.numero = estilo(wb, null, TINTA, 10, false, true, HorizontalAlignment.RIGHT, FORMATO_NUMERO, false);
        e.numeroBanda = estilo(wb, BANDA, TINTA, 10, false, true, HorizontalAlignment.RIGHT, FORMATO_NUMERO, false);
        e.numeroFinal = estilo(wb, AMARILLO_SUAVE, TINTA, 10, true, true, HorizontalAlignment.RIGHT, FORMATO_NUMERO, false);
        e.totalTexto = estiloTotal(wb, GRIS, HorizontalAlignment.LEFT, null);
        e.totalNumero = estiloTotal(wb, GRIS, HorizontalAlignment.RIGHT, FORMATO_NUMERO);
        e.totalFinal = estiloTotal(wb, AMARILLO, HorizontalAlignment.RIGHT, FORMATO_NUMERO);
        byte[] oscuro = {(byte) 0xDD, (byte) 0xE5, (byte) 0xDA};
        e.generalTexto = estiloTotal(wb, oscuro, HorizontalAlignment.LEFT, null);
        e.generalNumero = estiloTotal(wb, oscuro, HorizontalAlignment.RIGHT, FORMATO_NUMERO);
        e.generalFinal = estiloTotal(wb, AMARILLO, HorizontalAlignment.RIGHT, FORMATO_NUMERO);
        return e;
    }

    /** Una fila de total: negrita, fondo propio y una regla gruesa arriba. */
    private XSSFCellStyle estiloTotal(XSSFWorkbook wb, byte[] fondo, HorizontalAlignment alineacion, String formato) {
        XSSFCellStyle s = estilo(wb, fondo, TINTA, 10, true, true, alineacion, formato, false);
        s.setBorderTop(BorderStyle.MEDIUM);
        s.setTopBorderColor(new XSSFColor(TINTA, null));
        return s;
    }

    private XSSFCellStyle estilo(XSSFWorkbook wb, byte[] fondo, byte[] tinta, int puntos, boolean negrita,
                                 boolean borde, HorizontalAlignment alineacion, String formato, boolean ajustar) {
        XSSFFont fuente = wb.createFont();
        fuente.setFontName("Calibri");
        fuente.setFontHeightInPoints((short) puntos);
        fuente.setBold(negrita);
        fuente.setColor(new XSSFColor(tinta, null));
        XSSFCellStyle s = wb.createCellStyle();
        s.setFont((Font) fuente);
        s.setAlignment(alineacion);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(ajustar);
        if (fondo != null) {
            s.setFillForegroundColor(new XSSFColor(fondo, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        if (borde) {
            for (BorderStyle b : new BorderStyle[] {BorderStyle.THIN}) {
                s.setBorderTop(b);
                s.setBorderBottom(b);
                s.setBorderLeft(b);
                s.setBorderRight(b);
            }
            XSSFColor gris = new XSSFColor(BORDE, null);
            s.setTopBorderColor(gris);
            s.setBottomBorderColor(gris);
            s.setLeftBorderColor(gris);
            s.setRightBorderColor(gris);
        }
        if (formato != null) s.setDataFormat(wb.createDataFormat().getFormat(formato));
        return s;
    }
}
