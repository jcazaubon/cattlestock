package com.kinetix.cattlestock.web;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kinetix.cattlestock.app.LedgerService;
import com.kinetix.cattlestock.data.CategoryEntity;
import com.kinetix.cattlestock.data.CategoryRepository;
import com.kinetix.cattlestock.data.EstablishmentEntity;
import com.kinetix.cattlestock.domain.DomainException;
import com.kinetix.cattlestock.domain.Movement;
import com.kinetix.cattlestock.domain.MovementType;

/** Alta de movimientos, de a uno. La carga en grilla va por /grilla. */
@Controller
public class AltaController {

    private final LedgerService service;
    private final CategoryRepository categories;
    private final CurrentUser currentUser;

    public AltaController(LedgerService service, CategoryRepository categories, CurrentUser currentUser) {
        this.service = service;
        this.categories = categories;
        this.currentUser = currentUser;
    }

    @GetMapping("/alta")
    public String alta(Model model,
                       @RequestParam(required = false) String error,
                       @RequestParam(required = false) String establecimiento,
                       @RequestParam(required = false) String categoria,
                       @RequestParam(required = false) String tipo,
                       @RequestParam(required = false) String fecha,
                       @RequestParam(required = false) String cantidad,
                       @RequestParam(required = false) String cueros,
                       @RequestParam(required = false) String nota,
                       @RequestParam(required = false) String destinoEstablecimiento,
                       @RequestParam(required = false) String destinoCategoria) {
        model.addAttribute("error", error);
        model.addAttribute("establecimiento", s(establecimiento));
        model.addAttribute("categoria", s(categoria));
        model.addAttribute("tipo", tipo);
        model.addAttribute("fecha", s(fecha));
        model.addAttribute("cantidad", s(cantidad));
        model.addAttribute("cueros", s(cueros));
        model.addAttribute("nota", s(nota));
        model.addAttribute("destinoEstablecimiento", s(destinoEstablecimiento));
        model.addAttribute("destinoCategoria", s(destinoCategoria));
        model.addAttribute("hoy", LocalDate.now().toString());
        model.addAttribute("establecimientos", currentUser.establecimientos());
        model.addAttribute("categorias", categoriasDe(establecimiento));
        model.addAttribute("destinoCategorias", categoriasDe(destinoEstablecimiento));
        return "alta";
    }

    @PostMapping("/movimientos")
    public String registrar(Model model,
                            @RequestParam String fecha,
                            @RequestParam String tipo,
                            @RequestParam String establecimiento,
                            @RequestParam String categoria,
                            @RequestParam String cantidad,
                            @RequestParam(required = false) String cueros,
                            @RequestParam(required = false) String nota,
                            @RequestParam(required = false) String destinoEstablecimiento,
                            @RequestParam(required = false) String destinoCategoria) {
        try {
            Movement m = construir(fecha, tipo, establecimiento, categoria, cantidad,
                    cueros, nota, destinoEstablecimiento, destinoCategoria);
            service.addMovement(m);
            return "redirect:/planilla?establecimiento=" + m.establishmentId()
                    + "&mes=" + YearMonth.from(m.date());
        } catch (DomainException | IllegalArgumentException | DateTimeParseException e) {
            return alta(model, e.getMessage(), establecimiento, categoria, tipo, fecha, cantidad, cueros, nota,
                    destinoEstablecimiento, destinoCategoria);
        }
    }

    // --- Fragmentos HTMX ---

    /** Al cambiar el origen se rehacen categorias, tipo y campos: el destino nunca ofrece el origen. */
    @GetMapping("/alta/origen")
    public String origen(@RequestParam(required = false) String establecimiento,
                         @RequestParam(required = false) String tipo,
                         @RequestParam(required = false) String categoria,
                         @RequestParam(required = false) String destinoEstablecimiento,
                         @RequestParam(required = false) String destinoCategoria,
                         Model model) {
        model.addAttribute("establecimiento", s(establecimiento));
        model.addAttribute("tipo", tipo);
        model.addAttribute("categoria", s(categoria));
        model.addAttribute("destinoEstablecimiento", s(destinoEstablecimiento));
        model.addAttribute("destinoCategoria", s(destinoCategoria));
        model.addAttribute("establecimientos", currentUser.establecimientos());
        model.addAttribute("categorias", categoriasVisibles(establecimiento));
        model.addAttribute("destinoCategorias", categoriasDe(destinoEstablecimiento));
        return "fragment/origen";
    }

    @GetMapping("/alta/destino-categorias")
    public String destinoCategorias(@RequestParam(required = false) String destinoEstablecimiento,
                                    @RequestParam(required = false) String destinoCategoria,
                                    Model model) {
        model.addAttribute("destinoCategorias", categoriasVisibles(destinoEstablecimiento));
        model.addAttribute("destinoCategoria", s(destinoCategoria));
        return "fragment/destinoCategorias";
    }

    @GetMapping("/alta/campos")
    public String campos(@RequestParam(required = false) String tipo,
                        @RequestParam(required = false) String establecimiento,
                        @RequestParam(required = false) String categoria,
                        @RequestParam(required = false) String destinoEstablecimiento,
                        @RequestParam(required = false) String destinoCategoria,
                        Model model) {
        model.addAttribute("tipo", tipo);
        model.addAttribute("establecimiento", s(establecimiento));
        model.addAttribute("categoria", s(categoria));
        model.addAttribute("destinoEstablecimiento", s(destinoEstablecimiento));
        model.addAttribute("destinoCategoria", s(destinoCategoria));
        model.addAttribute("establecimientos", currentUser.establecimientos());
        model.addAttribute("categorias", categoriasVisibles(establecimiento));
        model.addAttribute("destinoCategorias", categoriasDe(destinoEstablecimiento));
        return "fragment/campos";
    }

    // --- Helpers ---

    private Movement construir(String fecha, String tipo, String establecimiento, String categoria,
                               String cantidad, String cueros, String nota,
                               String destinoEstablecimiento, String destinoCategoria) {
        String est = texto(establecimiento, "Elegi el establecimiento");
        if (!currentUser.puedeAcceder(est)) {
            throw new IllegalArgumentException("No podes cargar en " + est);
        }
        LocalDate d;
        try {
            d = LocalDate.parse(texto(fecha, "Elegi la fecha"));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha no es valida");
        }
        MovementType t;
        try {
            t = MovementType.valueOf(texto(tipo, "Elegi el tipo"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El tipo no es valido");
        }
        int q;
        try {
            q = Integer.parseInt(texto(cantidad, "Escribi la cantidad").trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La cantidad debe ser un numero entero");
        }
        String cat = texto(categoria, "Elegi la categoria");
        String id = UUID.randomUUID().toString();
        String note = nota == null || nota.isBlank() ? null : nota.trim();
        return switch (t) {
            case COMPRA -> Movement.compra(id, d, est, cat, q);
            case VENTA -> Movement.venta(id, d, est, cat, q);
            case NACIMIENTO -> Movement.nacimiento(id, d, est, cat, q);
            case CONSUMO -> Movement.consumo(id, d, est, cat, q);
            case MORTANDAD -> Movement.mortandad(id, d, est, cat, q,
                    cueros == null || cueros.isBlank() ? 0 : Integer.parseInt(cueros.trim()));
            case CAMBIO_CATEGORIA -> Movement.cambioCategoria(id, d, est, cat,
                    texto(destinoCategoria, "Elegi la categoria destino"), q);
            case TRASLADO -> Movement.traslado(id, d, est, cat,
                    texto(destinoEstablecimiento, "Elegi el establecimiento destino"),
                    texto(destinoCategoria, "Elegi la categoria destino"), q);
        };
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
        return categoriasDe(establecimiento);
    }

    private List<CategoryEntity> categoriasDe(String establecimiento) {
        if (establecimiento == null || establecimiento.isBlank()) return List.of();
        return categories.findByEstablishmentCodeOrderByPositionAsc(establecimiento);
    }

    private static String s(String valor) {
        return valor == null ? "" : valor;
    }
}
