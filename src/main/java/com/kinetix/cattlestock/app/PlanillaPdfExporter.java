package com.kinetix.cattlestock.app;

import java.awt.Color;
import java.io.OutputStream;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.kinetix.cattlestock.domain.Column;
import com.kinetix.cattlestock.domain.Planilla;
import com.kinetix.cattlestock.domain.Species;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Arma la planilla mensual en PDF, en una hoja A4 horizontal: una tabla por especie con los mismos grupos de
 * columnas que la pantalla y el Excel (entradas en verde, salidas en rojo, existencia final resaltada).
 */
@Component
public class PlanillaPdfExporter {

    private static final Column[] COLUMNAS = {
            Column.COMPRA, Column.NACIMIENTO, Column.TRASLADO_ENTRA, Column.CAMBIO_ENTRA, Column.RECUENTO_MAS,
            Column.VENTA, Column.MORTANDAD, Column.CONSUMO, Column.PASTOREO, Column.TRASLADO_SALE,
            Column.CAMBIO_SALE, Column.RECUENTO_MENOS };

    private static final String[] TITULOS = {
            "Compra", "Nacim.", "Transf. A", "Cambio A", "Dif. rec. +",
            "Venta", "Mortand.", "Consumo", "Pastoreo", "Transf. DE", "Cambio DE", "Dif. rec. -" };

    private static final int ENTRADAS = 5;

    private static final Color TINTA = new Color(0x14, 0x26, 0x2B);
    private static final Color GRIS = new Color(0xF1, 0xF4, 0xEF);
    private static final Color VERDE_SUAVE = new Color(0xE3, 0xF0, 0xE6);
    private static final Color ROJO_SUAVE = new Color(0xFB, 0xEA, 0xE7);
    private static final Color AMARILLO_SUAVE = new Color(0xFD, 0xF3, 0xCC);
    private static final Color BORDE = new Color(0xB9, 0xC4, 0xBB);

    public void write(Planilla planilla, OutputStream out) {
        Document doc = new Document(PageSize.A4.rotate(), 28, 28, 28, 36);
        try {
            PdfWriter.getInstance(doc, out);
            doc.addTitle("Planilla " + planilla.establishment().name() + " " + planilla.month());
            doc.addCreator("CattleStock");
            doc.open();

            String mes = planilla.month().getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"))
                    + " " + planilla.month().getYear();
            doc.add(new Paragraph(planilla.establishment().name() + " - " + mes
                    + (planilla.closed() ? " (cerrada)" : ""),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, TINTA)));
            Paragraph sub = new Paragraph("Planilla mensual de hacienda: existencia anterior, movimientos del mes y existencia final",
                    FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color.GRAY));
            sub.setSpacingAfter(8);
            doc.add(sub);

            for (Species especie : Species.values()) {
                List<Planilla.Row> filas = planilla.rows().stream()
                        .filter(r -> r.category().species() == especie).toList();
                if (filas.isEmpty()) continue;
                doc.add(tabla(etiqueta(especie), filas, planilla.totalFor(especie)));
            }

            Planilla.Totals g = planilla.totalGeneral();
            Paragraph total = new Paragraph("Total general: anterior " + g.anterior() + ", entradas " + g.entradas() + ", salidas " + g.salidas()
                    + ", final " + g.finalStock(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, TINTA));
            total.setSpacingBefore(4);
            doc.add(total);
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo armar el PDF de la planilla", e);
        } finally {
            if (doc.isOpen()) doc.close();
        }
    }

    private PdfPTable tabla(String especie, List<Planilla.Row> filas, Planilla.Totals totales) throws DocumentException {
        int columnas = 2 + COLUMNAS.length + 3;
        PdfPTable t = new PdfPTable(columnas);
        t.setWidthPercentage(100);
        float[] anchos = new float[columnas];
        anchos[0] = 4.2f;
        for (int i = 1; i < columnas; i++) anchos[i] = 1.4f;
        t.setWidths(anchos);
        t.setSpacingAfter(8);
        t.setKeepTogether(false);

        PdfPCell rotulo = celda(especie, Element.ALIGN_LEFT, true, GRIS, 9.5f, TINTA);
        rotulo.setColspan(columnas);
        t.addCell(rotulo);

        t.addCell(celda("Categoría", Element.ALIGN_LEFT, true, GRIS, 7, TINTA));
        t.addCell(celda("Anterior", Element.ALIGN_RIGHT, true, GRIS, 7, TINTA));
        for (int i = 0; i < TITULOS.length; i++) {
            t.addCell(celda(TITULOS[i], Element.ALIGN_RIGHT, true, i < ENTRADAS ? VERDE_SUAVE : ROJO_SUAVE, 7, TINTA));
        }
        t.addCell(celda("Entradas", Element.ALIGN_RIGHT, true, VERDE_SUAVE, 7, TINTA));
        t.addCell(celda("Salidas", Element.ALIGN_RIGHT, true, ROJO_SUAVE, 7, TINTA));
        t.addCell(celda("Final", Element.ALIGN_RIGHT, true, AMARILLO_SUAVE, 7, TINTA));
        t.setHeaderRows(2);

        for (Planilla.Row f : filas) {
            t.addCell(celda(f.category().name(), Element.ALIGN_LEFT, false, null, 8, TINTA));
            t.addCell(num(f.anterior(), false, null));
            for (Column c : COLUMNAS) t.addCell(num(f.columns().get(c), false, null));
            t.addCell(num(f.entradas(), false, null));
            t.addCell(num(f.salidas(), false, null));
            t.addCell(num(f.finalStock(), true, AMARILLO_SUAVE));
        }

        t.addCell(celda("Total", Element.ALIGN_LEFT, true, GRIS, 8, TINTA));
        t.addCell(num(totales.anterior(), true, GRIS));
        int entradas = 0;
        int salidas = 0;
        for (Column c : COLUMNAS) {
            int v = totales.columns().get(c);
            if (c.entrada) entradas += v; else salidas += v;
            t.addCell(num(v, true, GRIS));
        }
        t.addCell(num(entradas, true, GRIS));
        t.addCell(num(salidas, true, GRIS));
        t.addCell(num(totales.finalStock(), true, AMARILLO_SUAVE));
        return t;
    }

    private static PdfPCell num(int valor, boolean negrita, Color fondo) {
        // el cero se muestra como guion, igual que en el Excel
        return celda(valor == 0 ? "-" : String.valueOf(valor), Element.ALIGN_RIGHT, negrita, fondo, 8, TINTA);
    }

    private static PdfPCell celda(String texto, int alineacion, boolean negrita, Color fondo, float tamano, Color color) {
        Font font = FontFactory.getFont(negrita ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, tamano, color);
        PdfPCell c = new PdfPCell(new Phrase(texto, font));
        c.setHorizontalAlignment(alineacion);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBorderColor(BORDE);
        c.setBorderWidth(0.5f);
        c.setPadding(3);
        if (fondo != null) c.setBackgroundColor(fondo);
        return c;
    }

    private static String etiqueta(Species especie) {
        return switch (especie) {
            case VACUNOS -> "Vacunos";
            case YEGUARIZOS -> "Yeguarizos";
            default -> "Lanares";
        };
    }
}
