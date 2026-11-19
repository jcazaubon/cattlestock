package com.kinetix.cattlestock.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.kinetix.cattlestock.domain.Species;

/** Detecta categorias en los formatos de planilla que usan las empresas, sin asumir uno solo. */
public class ImportadorCategoriasTest {

    final ImportadorCategorias importador = new ImportadorCategorias();

    static void fila(Sheet hoja, int numero, Object... celdas) {
        Row r = hoja.createRow(numero);
        for (int i = 0; i < celdas.length; i++) {
            if (celdas[i] instanceof Number n) r.createCell(i).setCellValue(n.doubleValue());
            else if (celdas[i] != null) r.createCell(i).setCellValue(celdas[i].toString());
        }
    }

    public static byte[] bytes(Workbook libro) throws IOException {
        try (libro; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            libro.write(out);
            return out.toByteArray();
        }
    }

    /** Formato La Mulita y compania: encabezado "Viene del mes anterior" y categorias en la primera columna. */
    public static Workbook formatoSalidaEntrada() {
        Workbook l = new XSSFWorkbook();
        Sheet h = l.createSheet("NOVIEMBRE 2026");
        fila(h, 0, "Planilla De Hacienda");
        fila(h, 1, "Movimiento de hacienda");
        fila(h, 2, null, "SALIDA");
        fila(h, 3, null, "Viene Del Mes Anterior", "Mortandad", "Consumo", "Venta", "Pastoreo");
        fila(h, 4, "Toros Braman", 11);
        fila(h, 5, "Vacas Cria", 4975);
        fila(h, 6, "Vaquillonas 1er servicio", 60);
        fila(h, 7, "Yeguarizos", 8);
        fila(h, 8, "TOTAL", 5054);
        fila(h, 9, "*Se incluyen las 600 de otro campo");
        return l;
    }

    @Test
    void detectaElFormatoDeSalidaYEntradaYSaltaTotalesYNotas() throws Exception {
        var lectura = importador.leer("planilla.xlsx", bytes(formatoSalidaEntrada()));
        assertEquals(List.of("Toros Braman", "Vacas Cria", "Vaquillonas 1er servicio", "Yeguarizos"), lectura.nombres());
        assertEquals("la hoja \"NOVIEMBRE 2026\"", lectura.origen());
    }

    @Test
    void detectaElFormatoDeExistenciaAnteriorConLaFilaDeFechas() throws Exception {
        Workbook l = new HSSFWorkbook(); // .xls, como la planilla de Tisa
        Sheet h = l.createSheet("Enero 2025");
        fila(h, 0, "TRES ISLAS S.A.");
        fila(h, 1, "Movimiento de hacienda");
        fila(h, 3, "Categoría", "Existencia Anterior", "Compras", "Nacidos", "Transferencias");
        fila(h, 4, null, null, null, null, "A", "De");
        fila(h, 5, "FECHAS", 45658);
        fila(h, 6, "Toros", 1);
        fila(h, 7, "Vaca Ordeñe", 315);
        fila(h, 8, "Vaquillona C/Servicio", 61);
        var lectura = importador.leer("tisa.xls", bytes(l));
        assertEquals(List.of("Toros", "Vaca Ordeñe", "Vaquillona C/Servicio"), lectura.nombres());
    }

    @Test
    void detectaCategoriasQueEstanEnLaSegundaColumna() throws Exception {
        Workbook l = new XSSFWorkbook(); // 7 Arboles: la primera columna esta vacia
        Sheet h = l.createSheet("Julio 2026");
        fila(h, 0, null, "ESTANCIA SIETE ARBOLES");
        fila(h, 2, null, "Categorías", "Total por categoría");
        fila(h, 3, null, "Toros", 193);
        fila(h, 4, null, "Vacas CUT", 118);
        var lectura = importador.leer("siete.xlsx", bytes(l));
        assertEquals(List.of("Toros", "Vacas CUT"), lectura.nombres());
    }

    @Test
    void usaLaUltimaHojaConCategoriasPorqueTieneLasVigentes() throws Exception {
        Workbook l = new XSSFWorkbook();
        Sheet vieja = l.createSheet("ENERO 2018");
        fila(vieja, 3, null, "Viene Del Mes Anterior");
        fila(vieja, 4, "Toros Antiguos", 3);
        Sheet nueva = l.createSheet("DICIEMBRE 2026");
        fila(nueva, 3, null, "Viene Del Mes Anterior");
        fila(nueva, 4, "Toros", 3);
        fila(nueva, 5, "Novillos", 9);
        l.createSheet("Hoja1"); // vacia, como en la planilla de PG
        assertEquals(List.of("Toros", "Novillos"), importador.leer("x.xlsx", bytes(l)).nombres());
    }

    @Test
    void ignoraLosRotulosDeFilaDeLasPlanillasTranspuestasYElEncabezadoProduccion() throws Exception {
        Workbook l = new XSSFWorkbook(); // El Albardo / Maria Eugenia: las filas son movimientos, no categorias
        Sheet h = l.createSheet("Hoja1");
        fila(h, 0, null, "Existencia Anterior");
        fila(h, 1, "Exist. Al 31-12-17", 5);
        fila(h, 2, "Por Cambio de Categ.", 0);
        fila(h, 3, "Por Marcacion", 1);
        fila(h, 4, "DETALLE DE SALIDAS:");
        fila(h, 5, "17/01/18 Mort.1 novillo tropa cola.");
        assertTrue(importador.leer("albardo.xlsx", bytes(l)).nombres().isEmpty());

        Workbook m = new XSSFWorkbook();
        Sheet g = m.createSheet("DICIEMBRE 2026");
        fila(g, 3, null, "Viene Del Mes Anterior");
        fila(g, 4, "Novillos", 5);
        fila(g, 5, "PRODUCCION");
        fila(g, 6, "Padrillos", 2);
        assertEquals(List.of("Novillos", "Padrillos"), importador.leer("x.xlsx", bytes(m)).nombres());
    }

    @Test
    void sinCategoriasDevuelveUnaListaVaciaEnVezDeFallar() throws Exception {
        Workbook l = new XSSFWorkbook(); // formato transpuesto: categorias en columnas, sin encabezado reconocible
        fila(l.createSheet("Hoja1"), 0, "PLANILLA", "TOROS", "TORITOS", "VACAS");
        assertTrue(importador.leer("albardo.xlsx", bytes(l)).nombres().isEmpty());
    }

    @Test
    void unArchivoQueNoEsExcelDaUnErrorEntendible() {
        IOException e = org.junit.jupiter.api.Assertions.assertThrows(IOException.class,
                () -> importador.leer("roto.xlsx", "no es un excel".getBytes(StandardCharsets.UTF_8)));
        assertTrue(e.getMessage().contains("No pude leer"), e.getMessage());
    }

    @Test
    void textoPegadoUnaPorLineaConOSinMasColumnas() {
        List<String> n = importador.deTexto("Categoria\nToros\n  Vacas   Plantel \nvacas plantel\n\n42\nTerneros;VACUNOS\nOvejas,2\n");
        assertEquals(List.of("Toros", "Vacas Plantel", "Terneros", "Ovejas"), n);
    }

    @Test
    void adivinaLaEspecieDelNombre() {
        assertEquals(Species.VACUNOS, ImportadorCategorias.adivinarEspecie("Vacas Plantel"));
        assertEquals(Species.YEGUARIZOS, ImportadorCategorias.adivinarEspecie("Yeguarizos"));
        assertEquals(Species.YEGUARIZOS, ImportadorCategorias.adivinarEspecie("Padrillos"));
        assertEquals(Species.LANARES, ImportadorCategorias.adivinarEspecie("Ovejas madres"));
        assertEquals(Species.LANARES, ImportadorCategorias.adivinarEspecie("Corderos"));
    }
}
