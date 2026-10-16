package com.kinetix.cattlestock.web;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.kinetix.cattlestock.domain.DomainException;
import com.kinetix.cattlestock.domain.Movement;
import com.kinetix.cattlestock.domain.MovementType;

/**
 * Carga de movimientos por lote: una fecha y un establecimiento para todo el lote,
 * una linea por movimiento, y se guarda todo junto o nada (addBatch del dominio).
 * Traslados y cambios de categoria van con su destino en la misma linea: partida doble.
 */
@Controller
public class AltaController {

    /** Una linea del lote tal como se edita en la pantalla. */
    public record LineaForm(int indice, String categoria, String tipo, String cantidad, String cueros,
                            String destinoEstablecimiento, String destinoCategoria) {
    }

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
                       @RequestParam(required = false) Map<String, String> params) {
        String establecimiento = s(params.get("establecimiento"));
        List<LineaForm> lineas = lineasDe(params);
        model.addAttribute("error", error);
        model.addAttribute("hoy", LocalDate.now().toString());
        model.addAttribute("establecimiento", establecimiento);
        model.addAttribute("fecha", s(params.get("fecha")));
        model.addAttribute("nota", s(params.get("nota")));
        model.addAttribute("lineas", lineas);
        model.addAttribute("categorias", categoriasVisibles(establecimiento));
        model.addAttribute("establecimientos", currentUser.establecimientos());
        model.addAttribute("destinoCategoriasPorIndice", destinoCategoriasDe(lineas));
        return "alta";
    }

    // --- Fragmentos HTMX ---

    /** El boton "+" pide una linea nueva con el indice siguiente a las que ya hay. */
    @GetMapping("/alta/linea")
    public String linea(@RequestParam(required = false) Map<String, String> params, Model model) {
        String establecimiento = s(params.get("establecimiento"));
        model.addAttribute("linea", new LineaForm(siguienteIndice(params), "", "", "", "", "", ""));
        model.addAttribute("categorias", categoriasVisibles(establecimiento));
        model.addAttribute("establecimientos", currentUser.establecimientos());
        model.addAttribute("establecimiento", establecimiento);
        model.addAttribute("destinoCategorias", List.of());
        return "fragment/linea";
    }

    /** Al cambiar el establecimiento, el lote arranca de cero con una linea. */
    @GetMapping("/alta/lineas")
    public String lineas(@RequestParam(required = false) String establecimiento, Model model) {
        model.addAttribute("lineas", List.of(new LineaForm(0, "", "", "", "", "", "")));
        model.addAttribute("categorias", categoriasVisibles(establecimiento));
        model.addAttribute("establecimientos", currentUser.establecimientos());
        model.addAttribute("establecimiento", s(establecimiento));
        model.addAttribute("destinoCategoriasPorIndice", Map.of(0, List.of()));
        return "fragment/lineas";
    }

    /** Los campos que dependen del tipo, por linea: destino de traslado/cambio o cueros. */
    @GetMapping("/alta/campos-de-linea")
    public String camposDeLinea(@RequestParam(required = false) Map<String, String> params, Model model) {
        int indice = primerIndice(params);
        String establecimiento = s(params.get("establecimiento"));
        String destinoEstablecimiento = s(params.get("destinoEstablecimiento" + indice));
        model.addAttribute("indice", indice);
        model.addAttribute("tipo", s(params.get("tipo" + indice)));
        model.addAttribute("categoria", s(params.get("categoria" + indice)));
        model.addAttribute("establecimiento", establecimiento);
        model.addAttribute("destinoEstablecimiento", destinoEstablecimiento);
        model.addAttribute("destinoCategoria", s(params.get("destinoCategoria" + indice)));
        model.addAttribute("cueros", s(params.get("cueros" + indice)));
        model.addAttribute("categorias", categoriasVisibles(establecimiento));
        model.addAttribute("establecimientos", currentUser.establecimientos());
        model.addAttribute("destinoCategorias", categoriasDe(destinoEstablecimiento));
        return "fragment/camposLinea";
    }

    // --- Guardar el lote ---

    @PostMapping("/movimientos")
    public String registrar(Model model, @RequestParam(required = false) Map<String, String> params) {
        try {
            LocalDate d = parseFecha(texto(params.get("fecha"), "Elegi la fecha"));
            String est = texto(params.get("establecimiento"), "Elegi el establecimiento");
            if (!currentUser.puedeAcceder(est)) {
                throw new IllegalArgumentException("No podes cargar en " + est);
            }
            String nota = notaDe(params.get("nota"));
            List<Movement> lote = new ArrayList<>();
            for (int i : indicesDeLinea(params)) {
                String tipo = params.get("tipo" + i);
                if (tipo == null || tipo.isBlank()) continue;
                lote.add(construir(d, est, new LineaForm(i,
                        s(params.get("categoria" + i)), tipo, s(params.get("cantidad" + i)),
                        s(params.get("cueros" + i)), s(params.get("destinoEstablecimiento" + i)),
                        s(params.get("destinoCategoria" + i))), nota));
            }
            if (lote.isEmpty()) {
                throw new IllegalArgumentException("No escribiste ninguna linea para guardar");
            }
            service.addBatch(lote);
            return "redirect:/planilla?establecimiento=" + est + "&mes=" + YearMonth.from(d);
        } catch (DomainException | IllegalArgumentException | DateTimeParseException e) {
            return alta(model, e.getMessage(), params);
        }
    }

    // --- Helpers ---

    private Movement construir(LocalDate fecha, String establecimiento, LineaForm linea, String nota) {
        String cat = texto(linea.categoria(), "Elegi la categoria");
        MovementType t;
        try {
            t = MovementType.valueOf(texto(linea.tipo(), "Elegi el tipo"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El tipo no es valido");
        }
        int q;
        try {
            q = Integer.parseInt(texto(linea.cantidad(), "Escribi la cantidad").trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La cantidad debe ser un numero entero");
        }
        String id = UUID.randomUUID().toString();
        String destinoEstablecimiento = null;
        String destinoCategoria = null;
        Integer cueros = null;
        switch (t) {
            case MORTANDAD -> cueros = cuerosDe(linea.cueros());
            case CAMBIO_CATEGORIA -> destinoCategoria = texto(linea.destinoCategoria(), "Elegi la categoria destino");
            case TRASLADO -> {
                destinoEstablecimiento = texto(linea.destinoEstablecimiento(), "Elegi el establecimiento destino");
                destinoCategoria = texto(linea.destinoCategoria(), "Elegi la categoria destino");
            }
            default -> { }
        }
        return new Movement(id, fecha, t, establecimiento, cat, q, destinoEstablecimiento, destinoCategoria, cueros, nota);
    }

    private int cuerosDe(String cueros) {
        if (cueros == null || cueros.isBlank()) return 0;
        try {
            return Integer.parseInt(cueros.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Los cueros deben ser un numero entero");
        }
    }

    private static String notaDe(String nota) {
        return nota == null || nota.isBlank() ? null : nota.trim();
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

    private static List<Integer> indicesDeLinea(Map<String, String> params) {
        return params.keySet().stream()
                .filter(k -> k.matches("tipo\\d+"))
                .map(k -> Integer.parseInt(k.substring(4)))
                .sorted()
                .toList();
    }

    private static int siguienteIndice(Map<String, String> params) {
        List<Integer> indices = indicesDeLinea(params);
        return indices.isEmpty() ? 0 : indices.get(indices.size() - 1) + 1;
    }

    private static int primerIndice(Map<String, String> params) {
        List<Integer> indices = indicesDeLinea(params);
        return indices.isEmpty() ? 0 : indices.get(0);
    }

    private static List<LineaForm> lineasDe(Map<String, String> params) {
        List<Integer> indices = indicesDeLinea(params);
        if (indices.isEmpty()) {
            return List.of(new LineaForm(0, "", "", "", "", "", ""));
        }
        List<LineaForm> lineas = new ArrayList<>();
        for (int i : indices) {
            lineas.add(new LineaForm(i,
                    s(params.get("categoria" + i)), s(params.get("tipo" + i)),
                    s(params.get("cantidad" + i)), s(params.get("cueros" + i)),
                    s(params.get("destinoEstablecimiento" + i)), s(params.get("destinoCategoria" + i))));
        }
        return lineas;
    }

    private Map<Integer, List<CategoryEntity>> destinoCategoriasDe(List<LineaForm> lineas) {
        Map<Integer, List<CategoryEntity>> mapa = new HashMap<>();
        for (LineaForm linea : lineas) {
            mapa.put(linea.indice(), categoriasDe(linea.destinoEstablecimiento()));
        }
        return mapa;
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
