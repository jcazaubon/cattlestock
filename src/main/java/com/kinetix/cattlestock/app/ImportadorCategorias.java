package com.kinetix.cattlestock.app;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import com.kinetix.cattlestock.domain.Species;

/**
 * Saca las categorias de una planilla de hacienda en Excel (.xlsx o .xls), de un archivo de texto o de un
 * texto pegado. Las planillas de cada empresa tienen su propio formato, asi que no se asume uno: se busca la
 * columna de categorias por su encabezado ("Categoria", "Viene del mes anterior", "Existencia anterior"...) y
 * se lee hacia abajo. Se prueban las hojas de la ultima a la primera, porque las mas nuevas tienen las
 * categorias vigentes. Lo detectado es una propuesta: la persona lo revisa antes de crear nada.
 */
@Component
public class ImportadorCategorias {

    /** Lo detectado y de donde salio (la hoja, o el archivo de texto). */
    public record Lectura(List<String> nombres, String origen) {
    }

    private static final Pattern ENCABEZADO = Pattern.compile("^categor(ia|ias)$");
    private static final Pattern ENCABEZADO_NUMEROS = Pattern.compile(
            "^(viene del mes anterior|existencia anterior|existencia inicial|existencia al.*|total por categor.*)$");
    private static final Pattern NO_ES_CATEGORIA = Pattern.compile(
            "^(total.*|fechas?|lluvias?.*|acumulado.*|observ.*|notas?.*|existencia.*|recuento.*|incluye.*|campos.*"
                    + "|categor(ia|ias)|nombre|produccion|mortandad|ventas?|compras?|consumo|nacimientos?|traslados?"
                    // rotulos de las planillas transpuestas: "Exist. Al 31-12-17", "Por Marcacion", "Salidas:"
                    + "|exist\\..*|por .*|detalle.*|salidas?:?|entradas?:?|.*:|[0-9]{1,2}/[0-9]{1,2}/[0-9]{2,4}.*)$");
    private static final Pattern NUMERO = Pattern.compile("^[-+]?[0-9.,]+$");
    private static final int FILAS_PARA_ENCABEZADO = 20;
    private static final int BLANCOS_PARA_CORTAR = 3;

    /** Lee un archivo subido. Los .xlsx y .xls se tratan como planillas; el resto, como texto. */
    public Lectura leer(String nombreArchivo, byte[] datos) throws IOException {
        String nombre = nombreArchivo == null ? "" : nombreArchivo.toLowerCase(Locale.ROOT);
        if (nombre.endsWith(".xlsx") || nombre.endsWith(".xls") || nombre.endsWith(".xlsm")) {
            return deExcel(datos);
        }
        return new Lectura(deTexto(new String(datos, StandardCharsets.UTF_8)), "el archivo");
    }

    /** Una categoria por linea; si trae mas columnas (separadas por ; , o tab) se usa la primera. */
    public List<String> deTexto(String texto) {
        List<String> out = new ArrayList<>();
        if (texto == null) return out;
        for (String linea : texto.split("\\R")) {
            String primera = linea.split("[;\\t,]", -1)[0];
            agregar(out, primera);
        }
        return out;
    }

    private Lectura deExcel(byte[] datos) throws IOException {
        ArchivoExcel.exigir(datos);
        try (Workbook libro = WorkbookFactory.create(new ByteArrayInputStream(datos))) {
            for (int i = libro.getNumberOfSheets() - 1; i >= 0; i--) {
                List<String> nombres = deHoja(libro.getSheetAt(i));
                if (!nombres.isEmpty()) {
                    return new Lectura(nombres, "la hoja \"" + libro.getSheetName(i).trim() + "\"");
                }
            }
        } catch (IOException | RuntimeException e) {
            throw new IOException("No pude leer el archivo como planilla de Excel", e);
        }
        return new Lectura(List.of(), "el archivo");
    }

    private List<String> deHoja(Sheet hoja) {
        DataFormatter formato = new DataFormatter();
        int fila = -1;
        int columna = -1;
        int ultima = Math.min(hoja.getLastRowNum(), FILAS_PARA_ENCABEZADO);
        // 1) un encabezado "Categoria" y, si no hay, 2) el de la primera columna de numeros
        for (Pattern buscado : List.of(ENCABEZADO, ENCABEZADO_NUMEROS)) {
            for (int r = hoja.getFirstRowNum(); r <= ultima && fila < 0; r++) {
                Row f = hoja.getRow(r);
                if (f == null) continue;
                for (int c = f.getFirstCellNum(); c >= 0 && c < f.getLastCellNum(); c++) {
                    String t = normalizar(formato.formatCellValue(f.getCell(c)));
                    if (buscado.matcher(t).matches()) {
                        fila = r;
                        columna = buscado == ENCABEZADO ? c : Math.max(0, c - 1);
                        break;
                    }
                }
            }
            if (fila >= 0) break;
        }
        List<String> out = new ArrayList<>();
        if (fila < 0) return out;

        int blancos = 0;
        for (int r = fila + 1; r <= hoja.getLastRowNum(); r++) {
            Row f = hoja.getRow(r);
            Cell celda = f == null ? null : f.getCell(columna);
            String texto = celda == null || celda.getCellType() == CellType.NUMERIC
                    ? "" : formato.formatCellValue(celda).trim();
            if (texto.isEmpty()) {
                if (++blancos >= BLANCOS_PARA_CORTAR && !out.isEmpty()) break;
                continue;
            }
            blancos = 0;
            agregar(out, texto);
        }
        return out;
    }

    /** Suma un nombre si parece una categoria: no un total, un numero, una nota ni un nombre repetido. */
    private void agregar(List<String> out, String crudo) {
        String nombre = crudo == null ? "" : crudo.trim().replaceAll("\\s+", " ");
        if (nombre.isEmpty() || nombre.length() > 60 || nombre.startsWith("*")) return;
        String n = normalizar(nombre);
        if (NUMERO.matcher(n).matches() || NO_ES_CATEGORIA.matcher(n).matches()) return;
        for (String ya : out) {
            if (normalizar(ya).equals(n)) return;
        }
        out.add(nombre);
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    /** La especie mas probable por el nombre: casi todo es vacuno; se reconocen yeguarizos y lanares. */
    public static Species adivinarEspecie(String nombre) {
        String n = normalizar(nombre);
        if (n.matches(".*(yegua|caballo|potr|padrill|burro|mula|equin).*")) return Species.YEGUARIZOS;
        if (n.matches(".*(oveja|ovino|carnero|cordero|borrego|capon|lanar|lana).*")) return Species.LANARES;
        return Species.VACUNOS;
    }
}
