package com.kinetix.cattlestock.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kinetix.cattlestock.app.LedgerService;
import com.kinetix.cattlestock.data.CategoryEntity;
import com.kinetix.cattlestock.data.CategoryRepository;
import com.kinetix.cattlestock.data.CompanyEntity;
import com.kinetix.cattlestock.data.CompanyRepository;
import com.kinetix.cattlestock.data.EstablishmentRepository;
import com.kinetix.cattlestock.data.OpeningEntity;
import com.kinetix.cattlestock.data.OpeningRepository;
import com.kinetix.cattlestock.domain.DomainException;
import com.kinetix.cattlestock.domain.Species;

/** Configuracion: empresa, establecimientos, categorias y stock inicial. */
@Controller
public class ConfiguracionController {

    /** Una categoria con su stock inicial ya cargado, para la tabla de configuracion. */
    public record FilaCategoria(CategoryEntity categoria, int cantidad) {
    }

    private final LedgerService service;
    private final CompanyRepository companies;
    private final EstablishmentRepository establishments;
    private final CategoryRepository categories;
    private final OpeningRepository openings;

    public ConfiguracionController(LedgerService service, CompanyRepository companies,
                                    EstablishmentRepository establishments, CategoryRepository categories,
                                    OpeningRepository openings) {
        this.service = service;
        this.companies = companies;
        this.establishments = establishments;
        this.categories = categories;
        this.openings = openings;
    }

    @GetMapping("/configuracion")
    public String configuracion(Model model,
                                 @RequestParam(required = false) String establecimiento,
                                 @RequestParam(required = false) String error) {
        String seleccionado = s(establecimiento);
        model.addAttribute("error", error);
        model.addAttribute("empresa", companies.findById(CompanyEntity.ID).orElse(null));
        model.addAttribute("establecimientos", establishments.findAllByOrderByCodeAsc());
        model.addAttribute("seleccionado", seleccionado);
        model.addAttribute("filas", filasDe(seleccionado));
        return "configuracion";
    }

    @PostMapping("/configuracion/empresa")
    public String empresa(@RequestParam String nombre, @RequestParam String mesInicial) {
        try {
            service.configureCompany(texto(nombre, "Escribi el nombre"), YearMonth.parse(texto(mesInicial, "Elegi el mes inicial")));
        } catch (DomainException | IllegalArgumentException | DateTimeParseException e) {
            return conError(e.getMessage());
        }
        return "redirect:/configuracion";
    }

    @PostMapping("/configuracion/establecimientos")
    public String crearEstablecimiento(@RequestParam String codigo, @RequestParam String nombre) {
        try {
            service.createEstablishment(texto(codigo, "Escribi el codigo"), texto(nombre, "Escribi el nombre"));
        } catch (DomainException | IllegalArgumentException e) {
            return conError(e.getMessage());
        }
        return "redirect:/configuracion?establecimiento=" + codigo;
    }

    @PostMapping("/configuracion/categorias")
    public String crearCategoria(@RequestParam String establecimiento, @RequestParam String codigo,
                                 @RequestParam String nombre, @RequestParam String especie) {
        try {
            Species sp;
            try {
                sp = Species.valueOf(especie);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Elegi la especie");
            }
            service.createCategory(establecimiento, texto(codigo, "Escribi el codigo"),
                    texto(nombre, "Escribi el nombre"), sp);
        } catch (DomainException | IllegalArgumentException e) {
            return "redirect:/configuracion?establecimiento=" + establecimiento
                    + "&error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        }
        return "redirect:/configuracion?establecimiento=" + establecimiento;
    }

    @PostMapping("/configuracion/stock")
    public String stock(@RequestParam String establecimiento, @RequestParam Map<String, String> valores) {
        try {
            for (Map.Entry<String, String> entrada : valores.entrySet()) {
                String clave = entrada.getKey();
                if (!clave.startsWith("stock_") || entrada.getValue() == null || entrada.getValue().isBlank()) {
                    continue;
                }
                String categoria = clave.substring("stock_".length());
                service.setOpening(establecimiento, categoria, Integer.parseInt(entrada.getValue().trim()));
            }
        } catch (DomainException | IllegalArgumentException e) {
            return "redirect:/configuracion?establecimiento=" + establecimiento
                    + "&error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        }
        return "redirect:/configuracion?establecimiento=" + establecimiento;
    }

    // --- Helpers ---

    private List<FilaCategoria> filasDe(String establecimiento) {
        if (establecimiento.isBlank()) return List.of();
        Map<String, Integer> stock = openings.findByEstablishmentCodeIn(List.of(establecimiento)).stream()
                .collect(Collectors.toMap(OpeningEntity::getCategoryCode, OpeningEntity::getQuantity));
        return categories.findByEstablishmentCodeOrderByPositionAsc(establecimiento).stream()
                .map(c -> new FilaCategoria(c, stock.getOrDefault(c.getCode(), -1)))
                .toList();
    }

    private String conError(String mensaje) {
        return "redirect:/configuracion?error=" + URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
    }

    private String texto(String valor, String mensajeSiFalta) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(mensajeSiFalta);
        return valor.trim();
    }

    private static String s(String valor) {
        return valor == null ? "" : valor;
    }
}
