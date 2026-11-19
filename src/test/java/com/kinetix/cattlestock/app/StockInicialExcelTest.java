package com.kinetix.cattlestock.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import com.kinetix.cattlestock.data.CategoryEntity;
import com.kinetix.cattlestock.data.EstablishmentEntity;
import com.kinetix.cattlestock.domain.Species;

/** La planilla de stock inicial: se descarga con las categorias, se completa y se vuelve a leer. */
class StockInicialExcelTest {

    final StockInicialExcel excel = new StockInicialExcel();

    final List<EstablishmentEntity> ests = List.of(
            new EstablishmentEntity(1L, "isla", "Isla Media Luna"),
            new EstablishmentEntity(1L, "sanandres", "San Andrés"));
    final List<CategoryEntity> cats = List.of(
            new CategoryEntity(1L, "toros", "Toros", Species.VACUNOS, 0),
            new CategoryEntity(1L, "vacascria", "Vacas Cría", Species.VACUNOS, 1),
            new CategoryEntity(1L, "padrillos", "Padrillos", Species.YEGUARIZOS, 2));

    Workbook abrir(byte[] datos) throws Exception {
        return WorkbookFactory.create(new ByteArrayInputStream(datos));
    }

    byte[] guardar(Workbook libro) throws Exception {
        try (libro; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            libro.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void laPlantillaTraeLasCategoriasEnFilasYUnEstablecimientoPorColumna() throws Exception {
        byte[] datos = excel.plantilla("La Mulita", ests, cats, Map.of(StockInicialExcel.clave("isla", "toros"), 51));
        try (Workbook libro = abrir(datos)) {
            Sheet hoja = libro.getSheet(StockInicialExcel.HOJA);
            Row cab = hoja.getRow(0);
            assertEquals("Categoría", cab.getCell(0).getStringCellValue());
            assertEquals("Isla Media Luna (isla)", cab.getCell(1).getStringCellValue());
            assertEquals("San Andrés (sanandres)", cab.getCell(2).getStringCellValue());
            assertEquals("Toros", hoja.getRow(1).getCell(0).getStringCellValue());
            assertEquals("Padrillos", hoja.getRow(3).getCell(0).getStringCellValue());
            // viene con lo ya cargado y el resto vacio
            assertEquals(51, hoja.getRow(1).getCell(1).getNumericCellValue());
            assertEquals(org.apache.poi.ss.usermodel.CellType.BLANK, hoja.getRow(2).getCell(1).getCellType());
            assertTrue(libro.getSheet("Instrucciones") != null, "trae una hoja de instrucciones");
        }
    }

    @Test
    void completarLaPlantillaYVolverAleerlaDaLosValoresYSaltaLasCeldasVacias() throws Exception {
        byte[] datos = excel.plantilla("La Mulita", ests, cats, Map.of());
        Workbook libro = abrir(datos);
        Sheet hoja = libro.getSheet(StockInicialExcel.HOJA);
        hoja.getRow(1).getCell(1).setCellValue(120);   // Toros en Isla
        hoja.getRow(2).getCell(2).setCellValue(0);     // Vacas Cria en San Andres: un cero cuenta
        hoja.getRow(3).getCell(1).setCellValue(8);     // Padrillos en Isla
        var lectura = excel.leer(guardar(libro), ests, cats);

        assertTrue(lectura.problemas().isEmpty(), lectura.problemas().toString());
        assertEquals(3, lectura.celdas().size(), "solo las celdas con numero");
        var toros = lectura.celdas().get(0);
        assertEquals("isla", toros.establecimientoCodigo());
        assertEquals("toros", toros.categoriaCodigo());
        assertEquals(120, toros.cantidad());
        assertEquals(0, lectura.celdas().get(1).cantidad());
        assertEquals("sanandres", lectura.celdas().get(1).establecimientoCodigo());
    }

    @Test
    void reconoceElEstablecimientoPorNombreSinCodigoYLaCategoriaSinImportarAcentosNiMayusculas() throws Exception {
        Workbook libro = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        Sheet h = libro.createSheet("Hoja1");
        Row cab = h.createRow(0);
        cab.createCell(0).setCellValue("CATEGORÍA");
        cab.createCell(1).setCellValue("san andres");      // sin codigo, sin acento
        Row f = h.createRow(1);
        f.createCell(0).setCellValue("  vacas cria ");     // sin acento, con espacios
        f.createCell(1).setCellValue("35");                // texto numerico
        var lectura = excel.leer(guardar(libro), ests, cats);
        assertTrue(lectura.problemas().isEmpty(), lectura.problemas().toString());
        assertEquals(1, lectura.celdas().size());
        assertEquals("sanandres", lectura.celdas().get(0).establecimientoCodigo());
        assertEquals("vacascria", lectura.celdas().get(0).categoriaCodigo());
        assertEquals(35, lectura.celdas().get(0).cantidad());
    }

    @Test
    void loQueSeIgnoraSeInformaYNuncaEnSilencio() throws Exception {
        byte[] datos = excel.plantilla("La Mulita", ests, cats, Map.of());
        Workbook libro = abrir(datos);
        Sheet hoja = libro.getSheet(StockInicialExcel.HOJA);
        hoja.getRow(0).createCell(3).setCellValue("Campo Fantasma (fantasma)");    // establecimiento que no existe
        hoja.getRow(1).getCell(1).setCellValue(-4);                                 // negativo
        hoja.getRow(2).getCell(1).setCellValue(2.5);                                // decimal
        hoja.getRow(3).getCell(1).setCellValue("muchos");                           // texto
        Row extra = hoja.createRow(4);                                              // categoria que no existe
        extra.createCell(0).setCellValue("Dinosaurios");
        extra.createCell(1).setCellValue(9);
        Row total = hoja.createRow(5);                                              // una fila de totales no se reclama
        total.createCell(0).setCellValue("TOTAL");
        total.createCell(1).setCellValue(100);

        var lectura = excel.leer(guardar(libro), ests, cats);
        assertTrue(lectura.celdas().isEmpty(), "ninguna celda valida");
        String todo = String.join("\n", lectura.problemas());
        assertTrue(todo.contains("Campo Fantasma"), todo);
        assertTrue(todo.contains("negativo"), todo);
        assertTrue(todo.contains("no es un numero entero") && todo.contains("2.5"), todo);
        assertTrue(todo.contains("muchos"), todo);
        assertTrue(todo.contains("Dinosaurios"), todo);
        assertEquals(5, lectura.problemas().size(), "el TOTAL no genera aviso: " + todo);
    }

    @Test
    void unArchivoQueNoEsExcelDaUnErrorEntendible() {
        var e = org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class,
                () -> excel.leer("no es un excel".getBytes(), ests, cats));
        assertTrue(e.getMessage().contains("No pude leer"), e.getMessage());
    }
}
