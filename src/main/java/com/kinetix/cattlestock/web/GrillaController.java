package com.kinetix.cattlestock.web;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kinetix.cattlestock.app.LedgerService;
import com.kinetix.cattlestock.data.CategoryEntity;
import com.kinetix.cattlestock.domain.DomainException;
import com.kinetix.cattlestock.domain.Movement;
import com.kinetix.cattlestock.domain.MovementType;

/**
 * Carga rapida en grilla: una fecha, una fila por categoria y columnas de los tipos simples.
 * Traslados y cambios de categoria van por el formulario de alta, porque necesitan destino.
 */
@Controller
public class GrillaController {

    private final LedgerService service;
    private final CurrentUser currentUser;

    public GrillaController(LedgerService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/grilla")
    public String grilla(Model model,
                        @RequestParam(required = false) String error,
                        @RequestParam(required = false) String establecimiento,
                        @RequestParam(required = false) String fecha,
                        @RequestParam(required = false) String nota,
                        @RequestParam(required = false) String especie,
                        @RequestParam(required = false) Map<String, String> valores,
                        jakarta.servlet.http.HttpSession sesion) {
        if (establecimiento == null) {
            establecimiento = EstablecimientoPorDefecto.elegir(currentUser.establecimientos(), sesion);
        }
        model.addAttribute("error", error);
        model.addAttribute("especie", especieValida(especie));
        model.addAttribute("establecimiento", s(establecimiento));
        model.addAttribute("fecha", s(fecha));
        model.addAttribute("nota", s(nota));
        model.addAttribute("hoy", LocalDate.now().toString());
        java.time.YearMonth periodo = FechaController.periodoDe(fecha, s(valores == null ? null : valores.get("periodo")));
        model.addAttribute("periodo", periodo.toString());
        model.addAttribute("fechaPorDefecto", com.kinetix.cattlestock.domain.DiasHabiles.porDefecto(periodo, LocalDate.now()).toString());
        model.addAttribute("establecimientos", currentUser.establecimientos());
        model.addAttribute("categorias", categoriasVisibles(establecimiento));
        model.addAttribute("valores", valores == null ? Map.of() : valores);
        return "grilla";
    }

    @GetMapping("/grilla/filas")
    public String filas(@RequestParam(required = false) String establecimiento,
                        @RequestParam(required = false) String especie,
                        @RequestParam(required = false) Map<String, String> valores,
                        Model model) {
        model.addAttribute("especie", especieValida(especie));
        model.addAttribute("categorias", categoriasVisibles(establecimiento));
        model.addAttribute("valores", valores == null ? Map.of() : valores);
        return "fragment/filasGrilla";
    }

    @PostMapping("/grilla")
    public String cargar(Model model,
                         @RequestParam String establecimiento,
                         @RequestParam String fecha,
                         @RequestParam(required = false) String nota,
                         @RequestParam Map<String, String> valores,
                         jakarta.servlet.http.HttpSession sesion) {
        try {
            LocalDate d = parseFecha(texto(fecha, "Elegí la fecha"));
            if (!currentUser.puedeAcceder(establecimiento)) {
                throw new IllegalArgumentException("No podés cargar en " + establecimiento);
            }
            List<Movement> lote = construir(establecimiento, d, nota, valores);
            if (lote.isEmpty()) {
                throw new IllegalArgumentException("No escribiste ninguna cantidad");
            }
            service.addBatch(lote);
            EstablecimientoPorDefecto.recordar(sesion, establecimiento);
            return "redirect:/planilla?establecimiento=" + establecimiento + "&mes=" + YearMonth.from(d);
        } catch (DomainException | IllegalArgumentException | DateTimeParseException e) {
            return grilla(model, e.getMessage(), establecimiento, fecha, nota, valores.get("especie"), valores, sesion);
        }
    }

    // --- Helpers ---

    private List<Movement> construir(String establecimiento, LocalDate fecha, String nota, Map<String, String> valores) {
        String note = nota == null || nota.isBlank() ? null : nota.trim();
        List<Movement> lote = new ArrayList<>();
        for (CategoryEntity c : categoriasVisibles(establecimiento)) {
            String cat = c.getCode();
            agregar(lote, establecimiento, cat, fecha, valores.get("compra_" + cat), MovementType.COMPRA, 0, note);
            agregar(lote, establecimiento, cat, fecha, valores.get("nacimiento_" + cat), MovementType.NACIMIENTO, 0, note);
            agregar(lote, establecimiento, cat, fecha, valores.get("venta_" + cat), MovementType.VENTA, 0, note);
            agregar(lote, establecimiento, cat, fecha, valores.get("consumo_" + cat), MovementType.CONSUMO, 0, note);
            agregar(lote, establecimiento, cat, fecha, valores.get("pastoreo_" + cat), MovementType.PASTOREO, 0, note);
            agregar(lote, establecimiento, cat, fecha, valores.get("mortandad_" + cat), MovementType.MORTANDAD,
                    numero(valores.get("cueros_" + cat)), note);
        }
        agregarRecuentos(lote, establecimiento, fecha, valores, note);
        return lote;
    }

    /**
     * El recuento se escribe como cantidad contada. La diferencia contra la existencia esperada
     * (la de la fecha mas lo que el mismo lote ya mueve) se asienta como sobran o faltan.
     */
    private void agregarRecuentos(List<Movement> lote, String establecimiento, LocalDate fecha,
                                  Map<String, String> valores, String nota) {
        Map<String, Integer> esperada = new java.util.HashMap<>();
        for (var fila : service.existencia(service.companyOf(establecimiento), fecha, establecimiento, null, null)) {
            esperada.put(fila.category().id(), fila.quantity());
        }
        for (Movement m : List.copyOf(lote)) {
            for (var e : m.effects()) esperada.merge(e.categoryId(), e.signed(), Integer::sum);
        }
        for (CategoryEntity c : categoriasVisibles(establecimiento)) {
            String texto = valores.get("recuento_" + c.getCode());
            if (texto == null || texto.isBlank()) continue;
            int diferencia = numero(texto) - esperada.getOrDefault(c.getCode(), 0);
            if (diferencia == 0) continue;
            lote.add(new Movement(UUID.randomUUID().toString(), fecha,
                    diferencia > 0 ? MovementType.RECUENTO_MAS : MovementType.RECUENTO_MENOS,
                    establecimiento, c.getCode(), Math.abs(diferencia), null, null, null, nota));
        }
    }

    private void agregar(List<Movement> lote, String establecimiento, String categoria, LocalDate fecha,
                         String cantidad, MovementType tipo, int cueros, String nota) {
        int q = numero(cantidad);
        if (q <= 0) return;
        lote.add(new Movement(UUID.randomUUID().toString(), fecha, tipo, establecimiento, categoria, q,
                null, null, tipo == MovementType.MORTANDAD ? cueros : null, nota));
    }

    private int numero(String valor) {
        if (valor == null || valor.isBlank()) return 0;
        int n;
        try {
            n = Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Las cantidades deben ser numeros enteros");
        }
        if (n < 0) throw new IllegalArgumentException("Las cantidades no pueden ser negativas");
        return n;
    }

    private LocalDate parseFecha(String fecha) {
        try {
            return LocalDate.parse(fecha);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha no es válida");
        }
    }

    private String texto(String valor, String mensajeSiFalta) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(mensajeSiFalta);
        return valor.trim();
    }

    private List<CategoryEntity> categoriasVisibles(String establecimiento) {
        if (establecimiento == null || establecimiento.isBlank()
                || !currentUser.puedeAcceder(establecimiento)) {
            return List.of();
        }
        return service.categoriasHabilitadasDe(establecimiento);
    }

    /** La especie del filtro, o vacio para todas: un valor raro del navegador no rompe la pantalla. */
    private static String especieValida(String especie) {
        for (com.kinetix.cattlestock.domain.Species sp : com.kinetix.cattlestock.domain.Species.values()) {
            if (sp.name().equals(especie)) return especie;
        }
        return "";
    }

    private static String s(String valor) {
        return valor == null ? "" : valor;
    }
}
