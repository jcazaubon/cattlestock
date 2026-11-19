package com.kinetix.cattlestock.app;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.kinetix.cattlestock.data.CategoryEntity;
import com.kinetix.cattlestock.data.EstablishmentEntity;

/**
 * Planilla de Excel para cargar el stock inicial de toda la empresa de una vez: una fila por categoria y una
 * columna por establecimiento. Se descarga ya con las categorias (y con el stock que haya cargado), se completa
 * solo con los numeros y se vuelve a subir. Una celda vacia no toca nada; un cero deja el stock en cero.
 */
@Component
public class StockInicialExcel {

    public static final String HOJA = "Stock inicial";

    /** Un valor leido del archivo, ya resuelto contra los establecimientos y categorias de la empresa. */
    public record Celda(String establecimientoCodigo, String establecimiento,
                        String categoriaCodigo, String categoria, int cantidad) {
    }

    /** Lo leido, mas los problemas que hicieron ignorar algo (nunca se ignora en silencio). */
    public record Lectura(List<Celda> celdas, List<String> problemas) {
    }

    /** El encabezado de un establecimiento: "Nombre (codigo)", para que no haya ambiguedad al volver a leerlo. */
    static String encabezado(EstablishmentEntity e) {
        return e.getName() + " (" + e.getCode() + ")";
    }

    /** Clave de un stock actual en el mapa que recibe la planilla: "establecimiento|categoria". */
    public static String clave(String establecimiento, String categoria) {
        return establecimiento + "|" + categoria;
    }

    public byte[] plantilla(String empresa, List<EstablishmentEntity> establecimientos,
                            List<CategoryEntity> categorias, Map<String, Integer> actuales) throws IOException {
        try (Workbook libro = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet(HOJA);

            Font negrita = libro.createFont();
            negrita.setBold(true);
            CellStyle encabezadoEstilo = libro.createCellStyle();
            encabezadoEstilo.setFont(negrita);
            encabezadoEstilo.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            encabezadoEstilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle numero = libro.createCellStyle();
            numero.setDataFormat(libro.createDataFormat().getFormat("0"));

            Row cabecera = hoja.createRow(0);
            Cell c0 = cabecera.createCell(0);
            c0.setCellValue("Categoría");
            c0.setCellStyle(encabezadoEstilo);
            for (int i = 0; i < establecimientos.size(); i++) {
                Cell c = cabecera.createCell(i + 1);
                c.setCellValue(encabezado(establecimientos.get(i)));
                c.setCellStyle(encabezadoEstilo);
            }
            for (int r = 0; r < categorias.size(); r++) {
                Row fila = hoja.createRow(r + 1);
                fila.createCell(0).setCellValue(categorias.get(r).getName());
                for (int i = 0; i < establecimientos.size(); i++) {
                    Cell c = fila.createCell(i + 1);
                    c.setCellStyle(numero);
                    Integer actual = actuales.get(clave(establecimientos.get(i).getCode(), categorias.get(r).getCode()));
                    if (actual != null) c.setCellValue(actual);
                }
            }
            hoja.createFreezePane(1, 1);
            hoja.setColumnWidth(0, 34 * 256);
            for (int i = 0; i < establecimientos.size(); i++) hoja.setColumnWidth(i + 1, 24 * 256);

            Sheet ayuda = libro.createSheet("Instrucciones");
            String[] lineas = {
                "Stock inicial de " + empresa,
                "",
                "1. Completa en la hoja \"" + HOJA + "\" la cantidad de animales de cada categoria en cada establecimiento.",
                "2. Una celda vacia deja la categoria fuera de ese establecimiento. Un 0 la incluye, con stock en cero.",
                "3. No cambies los nombres de las categorias ni los encabezados de los establecimientos.",
                "4. Guarda el archivo y subilo en Configuracion, pestana Stock inicial.",
                "",
                "Antes de aplicar nada vas a ver lo que se va a cambiar."};
            for (int i = 0; i < lineas.length; i++) ayuda.createRow(i).createCell(0).setCellValue(lineas[i]);
            ayuda.setColumnWidth(0, 100 * 256);

            libro.write(out);
            return out.toByteArray();
        }
    }

    public Lectura leer(byte[] datos, List<EstablishmentEntity> establecimientos,
                        List<CategoryEntity> categorias) throws IOException {
        try (Workbook libro = WorkbookFactory.create(new ByteArrayInputStream(datos))) {
            Sheet hoja = libro.getSheet(HOJA);
            if (hoja == null) hoja = libro.getSheetAt(0);
            return leerHoja(hoja, establecimientos, categorias);
        } catch (IOException | RuntimeException e) {
            throw new IOException("No pude leer el archivo como planilla de Excel", e);
        }
    }

    private Lectura leerHoja(Sheet hoja, List<EstablishmentEntity> establecimientos, List<CategoryEntity> categorias) {
        DataFormatter formato = new DataFormatter();
        List<String> problemas = new ArrayList<>();

        // la fila de encabezados: la que dice "Categoria" en la primera celda con texto (por defecto, la primera)
        int filaCabecera = hoja.getFirstRowNum();
        for (int r = hoja.getFirstRowNum(); r <= Math.min(hoja.getLastRowNum(), 10); r++) {
            Row f = hoja.getRow(r);
            if (f != null && normalizar(formato.formatCellValue(f.getCell(0))).matches("categorias?")) {
                filaCabecera = r;
                break;
            }
        }
        Row cabecera = hoja.getRow(filaCabecera);
        if (cabecera == null) return new Lectura(List.of(), List.of("La hoja esta vacia"));

        // columnas -> establecimiento, por el codigo entre parentesis o, si no, por el nombre
        Map<Integer, EstablishmentEntity> columnas = new LinkedHashMap<>();
        Pattern conCodigo = Pattern.compile("\\(([^()]+)\\)\\s*$");
        for (int c = 1; c < cabecera.getLastCellNum(); c++) {
            String texto = formato.formatCellValue(cabecera.getCell(c)).trim();
            if (texto.isEmpty()) continue;
            EstablishmentEntity e = null;
            Matcher m = conCodigo.matcher(texto);
            String codigo = m.find() ? m.group(1).trim() : null;
            for (EstablishmentEntity candidato : establecimientos) {
                if ((codigo != null && candidato.getCode().equalsIgnoreCase(codigo))
                        || normalizar(candidato.getName()).equals(normalizar(texto))) {
                    e = candidato;
                    break;
                }
            }
            if (e == null) problemas.add("El establecimiento \"" + texto + "\" (columna " + (c + 1)
                    + ") no existe en la empresa: se ignora esa columna.");
            else columnas.put(c, e);
        }
        if (columnas.isEmpty() && problemas.isEmpty()) problemas.add("No encontre columnas de establecimientos");

        Map<String, CategoryEntity> porNombre = new LinkedHashMap<>();
        for (CategoryEntity c : categorias) porNombre.put(normalizar(c.getName()), c);

        // un mapa para que si una celda esta repetida gane la ultima, sin duplicar
        Map<String, Celda> resultado = new LinkedHashMap<>();
        for (int r = filaCabecera + 1; r <= hoja.getLastRowNum(); r++) {
            Row fila = hoja.getRow(r);
            if (fila == null) continue;
            String nombre = formato.formatCellValue(fila.getCell(0)).trim();
            if (nombre.isEmpty() || normalizar(nombre).startsWith("total")) continue;
            CategoryEntity categoria = porNombre.get(normalizar(nombre));
            boolean traeValores = false;
            for (int c : columnas.keySet()) {
                Cell celda = fila.getCell(c);
                if (celda != null && celda.getCellType() != CellType.BLANK
                        && !formato.formatCellValue(celda).trim().isEmpty()) traeValores = true;
            }
            if (categoria == null) {
                if (traeValores) problemas.add("La categoria \"" + nombre + "\" (fila " + (r + 1)
                        + ") no existe en la empresa: se ignora la fila.");
                continue;
            }
            for (Map.Entry<Integer, EstablishmentEntity> col : columnas.entrySet()) {
                Cell celda = fila.getCell(col.getKey());
                if (celda == null) continue;
                String texto = formato.formatCellValue(celda).trim();
                if (texto.isEmpty()) continue;
                Integer cantidad = entero(celda, texto);
                if (celda.getCellType() == CellType.NUMERIC) texto = String.valueOf(celda.getNumericCellValue());
                String donde = "\"" + categoria.getName() + "\" en " + col.getValue().getName() + " (fila " + (r + 1) + ")";
                if (cantidad == null) {
                    problemas.add("El valor \"" + texto + "\" de " + donde + " no es un numero entero: se ignora.");
                } else if (cantidad < 0) {
                    problemas.add("El valor " + cantidad + " de " + donde + " es negativo: se ignora.");
                } else {
                    resultado.put(clave(col.getValue().getCode(), categoria.getCode()),
                            new Celda(col.getValue().getCode(), col.getValue().getName(),
                                    categoria.getCode(), categoria.getName(), cantidad));
                }
            }
        }
        return new Lectura(new ArrayList<>(resultado.values()), problemas);
    }

    /** El entero de una celda, o null si no lo es (un decimal con parte fraccionaria o un texto). */
    private static Integer entero(Cell celda, String texto) {
        double valor;
        if (celda.getCellType() == CellType.NUMERIC) {
            valor = celda.getNumericCellValue();
        } else {
            try {
                valor = Double.parseDouble(texto.replace(".", "").replace(",", "."));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (valor != Math.rint(valor) || Math.abs(valor) > 1_000_000_000) return null;
        return (int) valor;
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
