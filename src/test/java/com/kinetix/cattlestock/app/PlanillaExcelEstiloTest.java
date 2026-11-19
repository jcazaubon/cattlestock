package com.kinetix.cattlestock.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.junit.jupiter.api.Test;

import com.kinetix.cattlestock.domain.Ledger;
import com.kinetix.cattlestock.domain.Movement;
import com.kinetix.cattlestock.domain.Species;

/** El Excel de la planilla tiene el aspecto de la app, sin perder ninguno de sus datos. */
class PlanillaExcelEstiloTest {

    static final YearMonth SEP = YearMonth.of(2026, 9);

    Workbook libro() throws Exception {
        Ledger l = new Ledger("Empresa de prueba", SEP);
        l.addEstablishment("P", "Principal");
        l.addCategory("P", "toros", "Toros", Species.VACUNOS);
        l.addCategory("P", "vacas", "Vacas", Species.VACUNOS);
        l.addCategory("P", "yeguarizos", "Yeguarizos", Species.YEGUARIZOS);
        l.setOpening("P", "toros", 51);
        l.setOpening("P", "yeguarizos", 58);
        l.add(Movement.compra("c1", LocalDate.of(2026, 9, 3), "P", "toros", 10));
        l.add(Movement.venta("v1", LocalDate.of(2026, 9, 9), "P", "toros", 4));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new PlanillaExcelExporter().write(l.planilla("P", SEP), out);
        return WorkbookFactory.create(new ByteArrayInputStream(out.toByteArray()));
    }

    Row filaCon(Sheet hoja, String texto) {
        for (Row r : hoja) {
            if (r.getCell(0) != null && r.getCell(0).getCellType() == CellType.STRING
                    && texto.equals(r.getCell(0).getStringCellValue())) return r;
        }
        throw new AssertionError("No se encontro la fila " + texto);
    }

    static String rgb(XSSFCellStyle s) {
        var c = s.getFillForegroundXSSFColor();
        if (c == null || c.getRGB() == null) return "sin relleno";
        StringBuilder sb = new StringBuilder();
        for (byte b : c.getRGB()) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    @Test
    void trae_unTituloConElMesEnEspanolYUnSubtitulo() throws Exception {
        try (Workbook wb = libro()) {
            Sheet hoja = wb.getSheetAt(0);
            assertEquals("Principal - septiembre 2026", hoja.getRow(0).getCell(0).getStringCellValue());
            assertTrue(hoja.getRow(1).getCell(0).getStringCellValue().startsWith("Planilla mensual de hacienda"));
            XSSFCellStyle titulo = (XSSFCellStyle) hoja.getRow(0).getCell(0).getCellStyle();
            assertEquals(15, titulo.getFont().getFontHeightInPoints());
            assertTrue(titulo.getFont().getBold());
        }
    }

    @Test
    void losEncabezadosVanAgrupadosYConElColorDeEntradasSalidasYFinal() throws Exception {
        try (Workbook wb = libro()) {
            Sheet hoja = wb.getSheetAt(0);
            Row grupo = filaCon(hoja, "Categoría");
            assertEquals("Entradas", grupo.getCell(2).getStringCellValue());
            assertEquals("Salidas", grupo.getCell(7).getStringCellValue());
            assertEquals("Final", grupo.getCell(16).getStringCellValue());
            // cada grupo tiene su color, y los rotulos de las columnas lo heredan
            String verde = rgb((XSSFCellStyle) grupo.getCell(2).getCellStyle());
            String rojo = rgb((XSSFCellStyle) grupo.getCell(7).getCellStyle());
            String amarillo = rgb((XSSFCellStyle) grupo.getCell(16).getCellStyle());
            assertEquals("E3F0E6", verde);
            assertEquals("FBEAE7", rojo);
            assertEquals("F2B705", amarillo);
            Row titulos = hoja.getRow(grupo.getRowNum() + 1);
            assertEquals("Compra", titulos.getCell(2).getStringCellValue());
            assertEquals(verde, rgb((XSSFCellStyle) titulos.getCell(2).getCellStyle()));
            assertEquals("Venta", titulos.getCell(7).getStringCellValue());
            assertEquals(rojo, rgb((XSSFCellStyle) titulos.getCell(7).getCellStyle()));
            // los grupos son celdas combinadas
            assertTrue(hoja.getMergedRegions().stream().anyMatch(m -> m.getFirstColumn() == 2 && m.getLastColumn() == 6),
                    "Entradas abarca sus 5 columnas");
            assertTrue(hoja.getMergedRegions().stream().anyMatch(m -> m.getFirstColumn() == 7 && m.getLastColumn() == 13),
                    "Salidas abarca sus 7 columnas");
        }
    }

    @Test
    void losNumerosTienenFormatoBordesYLaExistenciaFinalResaltada() throws Exception {
        try (Workbook wb = libro()) {
            Sheet hoja = wb.getSheetAt(0);
            Row toros = filaCon(hoja, "Toros");
            // los valores no cambiaron
            assertEquals(51, toros.getCell(1).getNumericCellValue());
            assertEquals(10, toros.getCell(2).getNumericCellValue());
            assertEquals(4, toros.getCell(7).getNumericCellValue());
            assertEquals(57, toros.getCell(16).getNumericCellValue());
            // pero se ven con separador de miles, el cero como guion y alineados a la derecha, con borde fino
            XSSFCellStyle num = (XSSFCellStyle) toros.getCell(1).getCellStyle();
            assertTrue(num.getDataFormatString().contains("#,##0"), num.getDataFormatString());
            assertTrue(num.getDataFormatString().contains("\"-\""), "el cero se ve como guion");
            assertEquals(org.apache.poi.ss.usermodel.HorizontalAlignment.RIGHT, num.getAlignment());
            assertEquals(BorderStyle.THIN, num.getBorderBottom());
            // la existencia final destaca con su propio fondo y negrita
            XSSFCellStyle fin = (XSSFCellStyle) toros.getCell(16).getCellStyle();
            assertEquals("FDF3CC", rgb(fin));
            assertTrue(fin.getFont().getBold());
            assertNotEquals(rgb(num), rgb(fin));
        }
    }

    @Test
    void lasFilasDeTotalTienenReglaGruesaYNegritaYElTotalGeneralSeDistingue() throws Exception {
        try (Workbook wb = libro()) {
            Sheet hoja = wb.getSheetAt(0);
            XSSFCellStyle total = (XSSFCellStyle) filaCon(hoja, "Total Vacunos").getCell(1).getCellStyle();
            assertEquals(BorderStyle.MEDIUM, total.getBorderTop());
            assertTrue(total.getFont().getBold());
            Row general = filaCon(hoja, "Total general");
            XSSFCellStyle g = (XSSFCellStyle) general.getCell(1).getCellStyle();
            assertNotEquals(rgb(total), rgb(g), "el total general tiene su propio fondo");
            // los totales de las columnas de entradas y salidas ahora tambien se completan
            Row totalVacunos = filaCon(hoja, "Total Vacunos");
            assertEquals(10, totalVacunos.getCell(14).getNumericCellValue(), "total de entradas");
            assertEquals(4, totalVacunos.getCell(15).getNumericCellValue(), "total de salidas");
            assertEquals(57, totalVacunos.getCell(16).getNumericCellValue());
            assertEquals(115, general.getCell(16).getNumericCellValue(), "57 vacunos + 58 yeguarizos");
        }
    }

    @Test
    void laHojaEstaLista_paraLeerYParaImprimir() throws Exception {
        try (Workbook wb = libro()) {
            Sheet hoja = wb.getSheetAt(0);
            assertTrue(hoja.getColumnWidth(0) > 30 * 256, "la columna de categorias entra completa");
            assertEquals(12 * 256, hoja.getColumnWidth(5));
            assertEquals(1, hoja.getPaneInformation().getVerticalSplitPosition(), "la columna de categorias queda fija");
            assertTrue(hoja.getPrintSetup().getLandscape(), "se imprime en horizontal");
            assertTrue(hoja.getFitToPage());
            assertEquals(1, hoja.getPrintSetup().getFitWidth(), "a un ancho de pagina");
            assertEquals(0, hoja.getPrintSetup().getFitHeight(), "y todo el alto que haga falta");
            assertTrue(hoja.getFooter().getCenter().contains("&P"), "numero de pagina");
            assertEquals(false, hoja.isDisplayGridlines(), "sin la cuadricula de fondo");
        }
    }

    @Test
    void lasFilasSeAlternanConUnaBandaSuaveParaLeerMejor() throws Exception {
        try (Workbook wb = libro()) {
            Sheet hoja = wb.getSheetAt(0);
            Cell primera = filaCon(hoja, "Toros").getCell(0);
            Cell segunda = filaCon(hoja, "Vacas").getCell(0);
            assertNotEquals(rgb((XSSFCellStyle) primera.getCellStyle()), rgb((XSSFCellStyle) segunda.getCellStyle()));
        }
    }
}
