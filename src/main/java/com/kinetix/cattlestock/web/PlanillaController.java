package com.kinetix.cattlestock.web;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kinetix.cattlestock.app.LedgerService;
import com.kinetix.cattlestock.app.PlanillaExcelExporter;
import com.kinetix.cattlestock.app.PlanillaPdfExporter;
import com.kinetix.cattlestock.data.EstablishmentEntity;
import com.kinetix.cattlestock.domain.DomainException;
import com.kinetix.cattlestock.domain.Planilla;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class PlanillaController {

    private final LedgerService service;
    private final CurrentUser currentUser;
    private final PlanillaExcelExporter excelExporter;
    private final PlanillaPdfExporter pdfExporter;

    public PlanillaController(LedgerService service, CurrentUser currentUser,
                              PlanillaExcelExporter excelExporter, PlanillaPdfExporter pdfExporter) {
        this.service = service;
        this.currentUser = currentUser;
        this.excelExporter = excelExporter;
        this.pdfExporter = pdfExporter;
    }

    @GetMapping("/planilla")
    public String planilla(Model model,
                           @RequestParam(required = false) String establecimiento,
                           @RequestParam(required = false) String mes,
                           @RequestParam(required = false) Integer mesNumero,
                           @RequestParam(required = false) Integer anio,
                           @RequestParam(required = false) String error) {
        var lista = currentUser.establecimientos();
        String code = establecimiento != null && !establecimiento.isBlank()
                ? establecimiento
                : (lista.isEmpty() ? null : lista.get(0).getCode());
        String mesStr = periodo(mes, mesNumero, anio);
        model.addAttribute("establecimientos", lista);
        model.addAttribute("establecimiento", code == null ? "" : code);
        model.addAttribute("mes", mesStr);
        agregarSelectorDePeriodo(model, mesStr);
        model.addAttribute("error", error);
        if (code == null) {
            return "planilla";
        }
        if (!currentUser.puedeAcceder(code)) {
            model.addAttribute("error", "No tenés acceso a ese establecimiento.");
            return "planilla";
        }
        try {
            model.addAttribute("planilla", service.planilla(code, YearMonth.parse(mesStr)));
        } catch (DomainException | DateTimeParseException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "planilla";
    }

    @GetMapping("/planilla/excel")
    public void excel(@RequestParam String establecimiento, @RequestParam String mes,
                      HttpServletResponse response) throws IOException {
        String volver = "/planilla?establecimiento=" + establecimiento + "&mes=" + mes;
        if (!currentUser.puedeAcceder(establecimiento)) {
            response.sendRedirect(volver + "&error=" + URLEncoder.encode("No tenés acceso a ese establecimiento.",
                    StandardCharsets.UTF_8));
            return;
        }
        Planilla planilla;
        try {
            planilla = service.planilla(establecimiento, YearMonth.parse(mes));
        } catch (DomainException | DateTimeParseException e) {
            response.sendRedirect(volver + "&error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
            return;
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=planilla-"
                + establecimiento + "-" + mes + ".xlsx");
        excelExporter.write(planilla, response.getOutputStream());
    }

    @GetMapping("/planilla/pdf")
    public void pdf(@RequestParam String establecimiento, @RequestParam String mes,
                    HttpServletResponse response) throws IOException {
        String volver = "/planilla?establecimiento=" + establecimiento + "&mes=" + mes;
        if (!currentUser.puedeAcceder(establecimiento)) {
            response.sendRedirect(volver + "&error=" + URLEncoder.encode("No tenés acceso a ese establecimiento.",
                    StandardCharsets.UTF_8));
            return;
        }
        Planilla planilla;
        try {
            planilla = service.planilla(establecimiento, YearMonth.parse(mes));
        } catch (DomainException | DateTimeParseException e) {
            response.sendRedirect(volver + "&error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
            return;
        }
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=planilla-"
                + establecimiento + "-" + mes + ".pdf");
        pdfExporter.write(planilla, response.getOutputStream());
    }

    /** El periodo pedido: mes y anio elegidos, o el texto aaaa-mm de los enlaces, o el mes actual por defecto. */
    static String periodo(String mes, Integer mesNumero, Integer anio) {
        if (mesNumero != null || anio != null) {
            int m = mesNumero != null && mesNumero >= 1 && mesNumero <= 12 ? mesNumero : YearMonth.now().getMonthValue();
            int a = anio != null && anio >= 1900 && anio <= 2200 ? anio : Year.now().getValue();
            return YearMonth.of(a, m).toString();
        }
        return mes != null && !mes.isBlank() ? mes : YearMonth.now().toString();
    }

    /** Los datos del selector de periodo: los doce meses, y los anios cercanos mas el pedido si queda fuera. */
    private static void agregarSelectorDePeriodo(Model model, String mesStr) {
        int mesActual;
        int anioActual;
        try {
            YearMonth ym = YearMonth.parse(mesStr);
            mesActual = ym.getMonthValue();
            anioActual = ym.getYear();
        } catch (DateTimeParseException e) {
            mesActual = YearMonth.now().getMonthValue();
            anioActual = Year.now().getValue();
        }
        List<String> meses = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            String nombre = java.time.Month.of(i).getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"));
            meses.add(Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1));
        }
        int hoy = Year.now().getValue();
        int desde = Math.min(hoy - 5, anioActual);
        int hasta = Math.max(hoy + 1, anioActual);
        List<Integer> anios = new ArrayList<>();
        for (int a = hasta; a >= desde; a--) anios.add(a);
        model.addAttribute("meses", meses);
        model.addAttribute("mesNumero", mesActual);
        model.addAttribute("anios", anios);
        model.addAttribute("anio", anioActual);
    }

    @PostMapping("/planilla/cerrar")
    public String cerrar(@RequestParam String establecimiento, @RequestParam String mes) {
        return accion(establecimiento, mes, () -> {
            if (!currentUser.puedeAcceder(establecimiento)) {
                throw new DomainException("No tenés acceso a ese establecimiento.");
            }
            service.closeMonth(establecimiento, YearMonth.parse(mes));
        });
    }

    @PostMapping("/planilla/reabrir")
    public String reabrir(@RequestParam String establecimiento, @RequestParam String mes) {
        return accion(establecimiento, mes, () -> {
            if (!currentUser.esAdmin()) {
                throw new DomainException("Solo un administrador puede reabrir una planilla.");
            }
            if (!currentUser.puedeAcceder(establecimiento)) {
                throw new DomainException("No tenés acceso a ese establecimiento.");
            }
            service.reopenLastMonth(establecimiento);
        });
    }

    private interface Accion {
        void run();
    }

    private String accion(String establecimiento, String mes, Accion accion) {
        String error = null;
        try {
            accion.run();
        } catch (DomainException | DateTimeParseException e) {
            error = e.getMessage();
        }
        return "redirect:/planilla?establecimiento=" + establecimiento + "&mes=" + mes
                + (error != null ? "&error=" + URLEncoder.encode(error, StandardCharsets.UTF_8) : "");
    }
}
