package com.kinetix.cattlestock.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kinetix.cattlestock.app.LedgerService;
import com.kinetix.cattlestock.data.CategoryEntity;
import com.kinetix.cattlestock.data.EstablishmentEntity;
import com.kinetix.cattlestock.data.MovementEntity;
import com.kinetix.cattlestock.data.UserEntity;
import com.kinetix.cattlestock.domain.DomainException;
import com.kinetix.cattlestock.domain.MovementType;

/**
 * Historial de movimientos, solo para administradores: ver los de un mes, corregirlos y borrarlos en forma logica
 * (quedan en el historial, no cuentan en el stock y se pueden restaurar). Cada cambio revalida todo el historial.
 */
@Controller
public class HistorialController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Un movimiento para la tabla, con los nombres ya resueltos. */
    public record Fila(Long id, String fecha, String fechaIso, String establecimiento, String tipo,
                       String categoriaCodigo, String categoria, int cantidad, Integer cueros, boolean mortandad,
                       boolean conDestino, String destino, String destinoCategoriaCodigo, String nota,
                       boolean eliminado, String auditoria) {
    }

    public record Opcion(String valor, String etiqueta) {
    }

    private final LedgerService service;
    private final CurrentUser currentUser;

    public HistorialController(LedgerService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/historial")
    public String historial(Model model,
                            @RequestParam(required = false) String establecimiento,
                            @RequestParam(required = false) String mes,
                            @RequestParam(required = false) String cat,
                            @RequestParam(required = false) String tipo,
                            @RequestParam(required = false) String eliminados,
                            @RequestParam(required = false) Long editar,
                            @RequestParam(required = false) String error,
                            @RequestParam(required = false) String aviso) {
        List<EstablishmentEntity> ests = currentUser.establecimientos();
        String estElegído = ests.stream().anyMatch(e -> e.getCode().equals(establecimiento)) ? establecimiento : "";
        YearMonth periodo;
        try {
            periodo = mes == null || mes.isBlank() ? YearMonth.now() : YearMonth.parse(mes);
        } catch (DateTimeParseException e) {
            periodo = YearMonth.now();
            error = "El mes no es valido";
        }
        boolean verEliminados = eliminados != null && !eliminados.isBlank();
        String catElegída = cat == null ? "" : cat;
        String tipoElegído = tipo == null ? "" : tipo;

        List<CategoryEntity> categorias = currentUser.empresaId().map(service::categoriasDeEmpresa).orElse(List.of());
        Map<String, String> nombreCat = categorias.stream()
                .collect(Collectors.toMap(CategoryEntity::getCode, CategoryEntity::getName, (a, b) -> a));
        Map<String, String> nombreEst = ests.stream()
                .collect(Collectors.toMap(EstablishmentEntity::getCode, EstablishmentEntity::getName));
        // para los destinos de traslado, que pueden ser de un establecimiento que el usuario no opera
        currentUser.establecimientosDeLaEmpresa().forEach(e -> nombreEst.putIfAbsent(e.getCode(), e.getName()));

        List<String> codigos = estElegído.isEmpty() ? ests.stream().map(EstablishmentEntity::getCode).toList()
                : List.of(estElegído);
        final String tipoFiltro = tipoElegído;
        final String catFiltro = catElegída;
        List<Fila> filas = service.listarMovimientos(codigos, periodo).stream()
                .filter(m -> verEliminados || !m.isDeleted())
                .filter(m -> tipoFiltro.isEmpty() || m.getType().name().equals(tipoFiltro))
                .filter(m -> catFiltro.isEmpty() || catFiltro.equals(m.getCategoryCode())
                        || catFiltro.equals(m.getDestCategoryCode()))
                .map(m -> fila(m, nombreCat, nombreEst))
                .toList();

        model.addAttribute("filas", filas);
        model.addAttribute("establecimientos", ests);
        model.addAttribute("establecimiento", estElegído);
        model.addAttribute("mes", periodo.toString());
        model.addAttribute("cat", catElegída);
        model.addAttribute("tipo", tipoElegído);
        model.addAttribute("eliminados", verEliminados);
        model.addAttribute("editar", editar == null ? "" : String.valueOf(editar));
        model.addAttribute("categorias", categorias);
        model.addAttribute("tipos", java.util.Arrays.stream(MovementType.values())
                .map(t -> new Opcion(t.name(), etiqueta(t))).toList());
        model.addAttribute("error", error);
        model.addAttribute("aviso", aviso);
        return "historial";
    }

    @PostMapping("/historial/{id}/editar")
    public String modificar(@PathVariable Long id,
                            @RequestParam String fecha, @RequestParam String categoria, @RequestParam String cantidad,
                            @RequestParam(required = false) String destinoCategoria,
                            @RequestParam(required = false) String cueros,
                            @RequestParam(required = false) String nota,
                            @RequestParam Map<String, String> filtros) {
        try {
            acceso(id);
            LocalDate d;
            try {
                d = LocalDate.parse(fecha);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("La fecha no es válida");
            }
            service.actualizarMovimiento(empresa(), id, usuario(), new LedgerService.CambiosMovimiento(d, categoria,
                    entero(cantidad, "La cantidad debe ser un numero entero"), destinoCategoria,
                    cueros == null || cueros.isBlank() ? null : entero(cueros, "Los cueros deben ser un numero entero"),
                    nota));
        } catch (DomainException | IllegalArgumentException e) {
            return volver(filtros, "&editar=" + id, "error", e.getMessage());
        }
        return volver(filtros, "", "aviso", "Movimiento actualizado.");
    }

    @PostMapping("/historial/{id}/eliminar")
    public String eliminar(@PathVariable Long id, @RequestParam Map<String, String> filtros) {
        try {
            acceso(id);
            service.eliminarMovimiento(empresa(), id, usuario());
        } catch (DomainException e) {
            return volver(filtros, "", "error", e.getMessage());
        }
        return volver(filtros, "", "aviso", "Movimiento eliminado. Queda en el historial y se puede restaurar.");
    }

    @PostMapping("/historial/{id}/restaurar")
    public String restaurar(@PathVariable Long id, @RequestParam Map<String, String> filtros) {
        try {
            acceso(id);
            service.restaurarMovimiento(empresa(), id);
        } catch (DomainException e) {
            return volver(filtros, "", "error", e.getMessage());
        }
        return volver(filtros, "", "aviso", "Movimiento restaurado.");
    }

    // --- Helpers ---

    private Long empresa() {
        return currentUser.empresaId().orElseThrow(() -> new DomainException("La empresa no esta configurada"));
    }

    private String usuario() {
        return currentUser.get().map(UserEntity::getUsername).orElse("");
    }

    /** El administrador solo toca movimientos de los establecimientos en los que opera. */
    private void acceso(Long id) {
        String est = service.establecimientoDeMovimiento(empresa(), id);
        if (!currentUser.puedeAcceder(est)) throw new DomainException("No tenés acceso a ese establecimiento.");
    }

    private static int entero(String valor, String mensaje) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    /** Vuelve al historial con los mismos filtros, y con el aviso o el error. */
    private String volver(Map<String, String> filtros, String extra, String clave, String mensaje) {
        StringBuilder url = new StringBuilder("redirect:/historial?");
        for (String nombre : List.of("establecimiento", "mes", "cat", "tipo", "eliminados")) {
            String valor = filtros.get(nombre);
            if (valor != null && !valor.isBlank()) {
                url.append(nombre).append('=').append(URLEncoder.encode(valor, StandardCharsets.UTF_8)).append('&');
            }
        }
        url.append(clave).append('=').append(URLEncoder.encode(mensaje, StandardCharsets.UTF_8)).append(extra);
        return url.toString();
    }

    private Fila fila(MovementEntity m, Map<String, String> nombreCat, Map<String, String> nombreEst) {
        String destino = "";
        if (m.getType() == MovementType.TRASLADO) {
            destino = "a " + nombreEst.getOrDefault(m.getDestEstablishmentCode(), m.getDestEstablishmentCode())
                    + ": " + nombreCat.getOrDefault(m.getDestCategoryCode(), m.getDestCategoryCode());
        } else if (m.getType() == MovementType.CAMBIO_CATEGORIA) {
            destino = "a " + nombreCat.getOrDefault(m.getDestCategoryCode(), m.getDestCategoryCode());
        }
        String auditoria = "";
        if (m.isDeleted()) {
            auditoria = "Eliminado por " + m.getDeletedBy() + " el " + m.getDeletedAt().format(FECHA_HORA);
        } else if (m.getModifiedBy() != null) {
            auditoria = "Editado por " + m.getModifiedBy() + " el " + m.getModifiedAt().format(FECHA_HORA);
        }
        return new Fila(m.getId(), m.getDate().format(FECHA), m.getDate().toString(),
                nombreEst.getOrDefault(m.getEstablishmentCode(), m.getEstablishmentCode()), etiqueta(m.getType()),
                m.getCategoryCode(), nombreCat.getOrDefault(m.getCategoryCode(), m.getCategoryCode()),
                m.getQuantity(), m.getCueros(), m.getType() == MovementType.MORTANDAD,
                m.getDestCategoryCode() != null, destino, m.getDestCategoryCode(),
                m.getNote() == null ? "" : m.getNote(), m.isDeleted(), auditoria);
    }

    static String etiqueta(MovementType t) {
        return switch (t) {
            case COMPRA -> "Compra";
            case VENTA -> "Venta";
            case NACIMIENTO -> "Nacimiento";
            case MORTANDAD -> "Mortandad";
            case CONSUMO -> "Consumo";
            case TRASLADO -> "Traslado";
            case CAMBIO_CATEGORIA -> "Cambio de categoria";
            case PASTOREO -> "Pastoreo";
            case RECUENTO_MAS -> "Recuento (sobran)";
            case RECUENTO_MENOS -> "Recuento (faltan)";
        };
    }
}
