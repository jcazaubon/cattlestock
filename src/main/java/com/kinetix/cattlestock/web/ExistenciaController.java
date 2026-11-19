package com.kinetix.cattlestock.web;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kinetix.cattlestock.app.LedgerService;
import com.kinetix.cattlestock.data.CategoryEntity;
import com.kinetix.cattlestock.data.EstablishmentEntity;
import com.kinetix.cattlestock.domain.Column;
import com.kinetix.cattlestock.domain.DomainException;
import com.kinetix.cattlestock.domain.Ledger;
import com.kinetix.cattlestock.domain.Species;

/** Vista general de existencia: agrupada por establecimiento y especie, con el movimiento del mes. */
@Controller
public class ExistenciaController {

    private static final List<Column> ENTRADAS = List.of(Column.COMPRA, Column.NACIMIENTO,
            Column.TRASLADO_ENTRA, Column.CAMBIO_ENTRA, Column.RECUENTO_MAS);
    private static final List<Column> SALIDAS = List.of(Column.VENTA, Column.MORTANDAD, Column.CONSUMO,
            Column.PASTOREO, Column.TRASLADO_SALE, Column.CAMBIO_SALE, Column.RECUENTO_MENOS);

    /** Una columna de movimiento que se muestra, con su titulo. */
    public record Col(Column columna, String titulo) {
    }

    /** Valor de una columna: el cero se muestra como guion para que se lean solo los movimientos. */
    private static String texto(Map<Column, Integer> columns, Column c) {
        int v = columns.getOrDefault(c, 0);
        return v == 0 ? "\u2013" : String.valueOf(v);
    }

    /** Totales de un grupo de filas. */
    public record Total(int inicio, Map<Column, Integer> columns, int existencia) {
        static Total vacio() {
            return new Total(0, new EnumMap<>(Column.class), 0);
        }

        public String texto(Column c) {
            return ExistenciaController.texto(columns, c);
        }
    }

    public record Fila(String categoria, int inicio, Map<Column, Integer> columns, int existencia, int porcentaje) {
        public String texto(Column c) {
            return ExistenciaController.texto(columns, c);
        }

        public String pctTexto() {
            return porcentaje == 0 && existencia > 0 ? "<1%" : porcentaje + "%";
        }
    }

    public record Seccion(String especie, List<Fila> filas, Total total) {
    }

    public record Bloque(String establecimiento, List<Seccion> secciones, Total total) {
    }

    /** Una parte del total, para el resumen: nombre, cantidad y porcentaje del total. */
    public record Parte(String nombre, int cantidad, int porcentaje) {
        public String pctTexto() {
            return porcentaje == 0 && cantidad > 0 ? "<1%" : porcentaje + "%";
        }

        /** Ancho de la barra: una parte chica pero real siempre se ve. */
        public int ancho() {
            return cantidad > 0 ? Math.max(1, porcentaje) : 0;
        }
    }

    /** Acumulador de una fila: sirve para sumar categorias iguales de distintos establecimientos. */
    private static final class Acumulado {
        final Species especie;
        final String nombre;
        int inicio;
        int existencia;
        final Map<Column, Integer> columns = new EnumMap<>(Column.class);

        Acumulado(Species especie, String nombre) {
            this.especie = especie;
            this.nombre = nombre;
        }

        void sumar(Ledger.ExistenceDetail f) {
            inicio += f.inicio();
            existencia += f.quantity();
            f.columns().forEach((c, q) -> columns.merge(c, q, Integer::sum));
        }

        void sumar(Acumulado otro) {
            inicio += otro.inicio;
            existencia += otro.existencia;
            otro.columns.forEach((c, q) -> columns.merge(c, q, Integer::sum));
        }

        Total total() {
            return new Total(inicio, columns, existencia);
        }
    }

    private static final java.time.format.DateTimeFormatter FORMATO = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final java.time.format.DateTimeFormatter FORMATO_CORTO = java.time.format.DateTimeFormatter.ofPattern("dd/MM");

    private final LedgerService service;
    private final CurrentUser currentUser;

    public ExistenciaController(LedgerService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/existencia")
    public String existencia(Model model,
                              @RequestParam(required = false) String establecimiento,
                              @RequestParam(required = false) String especie,
                              @RequestParam(required = false) String categoria,
                              @RequestParam(required = false) String fecha,
                              @RequestParam(required = false) String conExistencia,
                              @RequestParam(required = false) String error) {
        String fechaStr = s(fecha).isBlank() ? LocalDate.now().toString() : fecha;
        model.addAttribute("establecimientos", currentUser.establecimientos());
        model.addAttribute("establecimiento", s(establecimiento));
        model.addAttribute("especie", s(especie));
        model.addAttribute("categoria", s(categoria));
        model.addAttribute("fecha", fechaStr);
        model.addAttribute("conExistencia", conExistencia != null);
        model.addAttribute("error", error);
        model.addAttribute("grupos", categoriasPorEspecie(establecimiento, especie));
        model.addAttribute("bloques", List.<Bloque>of());
        model.addAttribute("porEstablecimiento", List.<Parte>of());
        model.addAttribute("porEspecie", List.<Parte>of());
        model.addAttribute("totalGeneral", Total.vacio());
        model.addAttribute("hayMovimiento", false);
        model.addAttribute("entradasVisibles", List.<Col>of());
        model.addAttribute("salidasVisibles", List.<Col>of());
        model.addAttribute("trasladosInternos", false);
        model.addAttribute("fechaTexto", "");
        model.addAttribute("inicioMesTexto", "");
        model.addAttribute("encontradas", 0);
        if (!s(establecimiento).isBlank() && !currentUser.puedeAcceder(establecimiento)) {
            model.addAttribute("error", "No tenés acceso a ese establecimiento.");
            return "existencia";
        }
        try {
            Species sp = s(especie).isBlank() ? null : Species.valueOf(especie);
            String est = s(establecimiento).isBlank() ? null : establecimiento;
            String cat = s(categoria).isBlank() ? null : categoria;
            LocalDate asOf = LocalDate.parse(fechaStr);
            Long empresa = currentUser.empresaId()
                    .orElseThrow(() -> new DomainException("La empresa no esta configurada"));
            List<Ledger.ExistenceDetail> filas = new ArrayList<>(
                    service.existenciaDetallada(empresa, asOf, est, sp, cat));
            // el usuario solo ve sus establecimientos, tambien cuando no eligio ninguno
            filas.removeIf(f -> !currentUser.puedeAcceder(f.establishment().id()));
            if (conExistencia != null) {
                filas.removeIf(f -> f.quantity() == 0 && f.entradas() == 0 && f.salidas() == 0);
            }
            armar(model, filas, asOf, est == null);
        } catch (DomainException | IllegalArgumentException | DateTimeParseException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "existencia";
    }

    /** Opciones del combo de categoria: solo las de los establecimientos que el usuario puede ver. */
    @GetMapping("/existencia/categorias")
    public String opcionesCategoria(Model model,
                                    @RequestParam(required = false) String establecimiento,
                                    @RequestParam(required = false) String especie,
                                    @RequestParam(required = false) String categoria) {
        model.addAttribute("grupos", categoriasPorEspecie(establecimiento, especie));
        model.addAttribute("categoria", s(categoria));
        return "fragment/opcionesCategoria";
    }

    // --- Armado de la vista ---

    private void armar(Model model, List<Ledger.ExistenceDetail> filas, LocalDate asOf, boolean global) {
        Acumulado todo = new Acumulado(null, "");
        for (Ledger.ExistenceDetail f : filas) todo.sumar(f);
        Total general = todo.total();

        // con "Todos" no se separa por establecimiento: una sola tabla, sumada por especie y categoria
        Map<String, Map<String, Acumulado>> porBloque = new LinkedHashMap<>();
        Map<String, Integer> porEstablecimiento = new LinkedHashMap<>();
        Map<Species, Integer> porEspecie = new EnumMap<>(Species.class);
        for (Ledger.ExistenceDetail f : filas) {
            String bloque = global ? "Todos los establecimientos" : f.establishment().name();
            String clave = f.category().species() + "|" + f.category().name().trim().toLowerCase();
            porBloque.computeIfAbsent(bloque, k -> new LinkedHashMap<>())
                    .computeIfAbsent(clave, k -> new Acumulado(f.category().species(), f.category().name()))
                    .sumar(f);
            porEstablecimiento.merge(f.establishment().name(), f.quantity(), Integer::sum);
            porEspecie.merge(f.category().species(), f.quantity(), Integer::sum);
        }

        List<Bloque> bloques = new ArrayList<>();
        for (var b : porBloque.entrySet()) {
            Map<Species, List<Acumulado>> secciones = new EnumMap<>(Species.class);
            Acumulado totalBloque = new Acumulado(null, "");
            for (Acumulado a : b.getValue().values()) {
                secciones.computeIfAbsent(a.especie, k -> new ArrayList<>()).add(a);
                totalBloque.sumar(a);
            }
            List<Seccion> lista = new ArrayList<>();
            for (var s : secciones.entrySet()) {
                Acumulado totalSeccion = new Acumulado(null, "");
                List<Fila> detalle = new ArrayList<>();
                for (Acumulado a : s.getValue()) {
                    detalle.add(new Fila(a.nombre, a.inicio, a.columns, a.existencia,
                            porcentaje(a.existencia, general.existencia())));
                    totalSeccion.sumar(a);
                }
                lista.add(new Seccion(etiqueta(s.getKey()), detalle, totalSeccion.total()));
            }
            bloques.add(new Bloque(b.getKey(), lista, totalBloque.total()));
        }

        List<Parte> partesEst = new ArrayList<>();
        porEstablecimiento.forEach((n, q) -> partesEst.add(new Parte(n, q, porcentaje(q, general.existencia()))));
        List<Parte> partesEsp = new ArrayList<>();
        porEspecie.forEach((sp, q) -> partesEsp.add(new Parte(etiqueta(sp), q, porcentaje(q, general.existencia()))));

        // solo se muestran las columnas en las que hubo movimiento
        List<Col> entradas = visibles(ENTRADAS, general.columns());
        List<Col> salidas = visibles(SALIDAS, general.columns());

        model.addAttribute("bloques", bloques);
        model.addAttribute("totalGeneral", general);
        model.addAttribute("encontradas", filas.size());
        // el desglose solo aparece cuando hay mas de un grupo para comparar
        model.addAttribute("porEstablecimiento", partesEst.size() > 1 ? partesEst : List.<Parte>of());
        model.addAttribute("porEspecie", partesEsp.size() > 1 ? partesEsp : List.<Parte>of());
        model.addAttribute("entradasVisibles", entradas);
        model.addAttribute("salidasVisibles", salidas);
        model.addAttribute("hayMovimiento", !entradas.isEmpty() || !salidas.isEmpty());
        model.addAttribute("trasladosInternos", global
                && general.columns().getOrDefault(Column.TRASLADO_ENTRA, 0) > 0
                && general.columns().getOrDefault(Column.TRASLADO_SALE, 0) > 0);
        model.addAttribute("fechaTexto", asOf.format(FORMATO));
        model.addAttribute("inicioMesTexto", asOf.withDayOfMonth(1).minusDays(1).format(FORMATO_CORTO));
    }

    private static List<Col> visibles(List<Column> candidatas, Map<Column, Integer> totales) {
        List<Col> out = new ArrayList<>();
        for (Column c : candidatas) {
            if (totales.getOrDefault(c, 0) > 0) out.add(new Col(c, titulo(c)));
        }
        return out;
    }

    private static String titulo(Column c) {
        return switch (c) {
            case COMPRA -> "Compra";
            case NACIMIENTO -> "Nacimiento";
            case TRASLADO_ENTRA, TRASLADO_SALE -> "Traslado";
            case CAMBIO_ENTRA, CAMBIO_SALE -> "Cambio de categoria";
            case RECUENTO_MAS -> "Recuento (sobran)";
            case RECUENTO_MENOS -> "Recuento (faltan)";
            case VENTA -> "Venta";
            case MORTANDAD -> "Mortandad";
            case CONSUMO -> "Consumo";
            case PASTOREO -> "Pastoreo";
        };
    }

    private static int porcentaje(int parte, int total) {
        return total <= 0 ? 0 : Math.round(parte * 100f / total);
    }

    private static String etiqueta(Species sp) {
        return sp == Species.VACUNOS ? "Vacunos" : sp == Species.YEGUARIZOS ? "Yeguarizos" : "Lanares";
    }

    /** Las categorias de la empresa agrupadas por especie: el catalogo es el mismo para todos sus establecimientos. */
    private Map<Species, List<CategoryEntity>> categoriasPorEspecie(String establecimiento, String especie) {
        Species filtro = null;
        try {
            filtro = s(especie).isBlank() ? null : Species.valueOf(especie);
        } catch (IllegalArgumentException e) {
            // una especie invalida se informa en la consulta principal
        }
        Map<Species, List<CategoryEntity>> porEspecie = new EnumMap<>(Species.class);
        for (CategoryEntity c : currentUser.empresaId().map(service::categoriasDeEmpresa).orElse(List.of())) {
            if (filtro != null && c.getSpecies() != filtro) continue;
            porEspecie.computeIfAbsent(c.getSpecies(), k -> new ArrayList<>()).add(c);
        }
        return porEspecie;
    }

    private static String s(String valor) {
        return valor == null ? "" : valor;
    }
}
