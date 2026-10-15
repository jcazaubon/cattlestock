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
import com.kinetix.cattlestock.data.CategoryRepository;
import com.kinetix.cattlestock.data.EstablishmentRepository;
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
    private final EstablishmentRepository establishments;
    private final CategoryRepository categories;

    public GrillaController(LedgerService service, EstablishmentRepository establishments, CategoryRepository categories) {
        this.service = service;
        this.establishments = establishments;
        this.categories = categories;
    }

    @GetMapping("/grilla")
    public String grilla(Model model,
                        @RequestParam(required = false) String error,
                        @RequestParam(required = false) String establecimiento,
                        @RequestParam(required = false) String fecha,
                        @RequestParam(required = false) String nota,
                        @RequestParam(required = false) Map<String, String> valores) {
        model.addAttribute("error", error);
        model.addAttribute("establecimiento", s(establecimiento));
        model.addAttribute("fecha", s(fecha));
        model.addAttribute("nota", s(nota));
        model.addAttribute("hoy", LocalDate.now().toString());
        model.addAttribute("establecimientos", establishments.findAllByOrderByCodeAsc());
        model.addAttribute("categorias", categoriasDe(establecimiento));
        model.addAttribute("valores", valores == null ? Map.of() : valores);
        return "grilla";
    }

    @GetMapping("/grilla/filas")
    public String filas(@RequestParam(required = false) String establecimiento, Model model) {
        model.addAttribute("categorias", categoriasDe(establecimiento));
        return "fragment/filasGrilla";
    }

    @PostMapping("/grilla")
    public String cargar(Model model,
                         @RequestParam String establecimiento,
                         @RequestParam String fecha,
                         @RequestParam(required = false) String nota,
                         @RequestParam Map<String, String> valores) {
        try {
            LocalDate d = parseFecha(texto(fecha, "Elegi la fecha"));
            List<Movement> lote = construir(establecimiento, d, nota, valores);
            if (lote.isEmpty()) {
                throw new IllegalArgumentException("No escribiste ninguna cantidad");
            }
            service.addBatch(lote);
            return "redirect:/planilla?establecimiento=" + establecimiento + "&mes=" + YearMonth.from(d);
        } catch (DomainException | IllegalArgumentException | DateTimeParseException e) {
            return grilla(model, e.getMessage(), establecimiento, fecha, nota, valores);
        }
    }

    // --- Helpers ---

    private List<Movement> construir(String establecimiento, LocalDate fecha, String nota, Map<String, String> valores) {
        String note = nota == null || nota.isBlank() ? null : nota.trim();
        List<Movement> lote = new ArrayList<>();
        for (CategoryEntity c : categoriasDe(establecimiento)) {
            String cat = c.getCode();
            agregar(lote, establecimiento, cat, fecha, valores.get("compra_" + cat), MovementType.COMPRA, 0, note);
            agregar(lote, establecimiento, cat, fecha, valores.get("nacimiento_" + cat), MovementType.NACIMIENTO, 0, note);
            agregar(lote, establecimiento, cat, fecha, valores.get("venta_" + cat), MovementType.VENTA, 0, note);
            agregar(lote, establecimiento, cat, fecha, valores.get("consumo_" + cat), MovementType.CONSUMO, 0, note);
            agregar(lote, establecimiento, cat, fecha, valores.get("mortandad_" + cat), MovementType.MORTANDAD,
                    numero(valores.get("cueros_" + cat)), note);
        }
        return lote;
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
            throw new IllegalArgumentException("La fecha no es valida");
        }
    }

    private String texto(String valor, String mensajeSiFalta) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(mensajeSiFalta);
        return valor.trim();
    }

    private List<CategoryEntity> categoriasDe(String establecimiento) {
        if (establecimiento == null || establecimiento.isBlank()) return List.of();
        return categories.findByEstablishmentCodeOrderByPositionAsc(establecimiento);
    }

    private static String s(String valor) {
        return valor == null ? "" : valor;
    }
}
