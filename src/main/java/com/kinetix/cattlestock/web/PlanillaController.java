package com.kinetix.cattlestock.web;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kinetix.cattlestock.app.LedgerService;
import com.kinetix.cattlestock.app.PlanillaExcelExporter;
import com.kinetix.cattlestock.data.EstablishmentEntity;
import com.kinetix.cattlestock.domain.DomainException;
import com.kinetix.cattlestock.domain.Planilla;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class PlanillaController {

    private final LedgerService service;
    private final CurrentUser currentUser;
    private final PlanillaExcelExporter excelExporter;

    public PlanillaController(LedgerService service, CurrentUser currentUser,
                              PlanillaExcelExporter excelExporter) {
        this.service = service;
        this.currentUser = currentUser;
        this.excelExporter = excelExporter;
    }

    @GetMapping("/planilla")
    public String planilla(Model model,
                           @RequestParam(required = false) String establecimiento,
                           @RequestParam(required = false) String mes,
                           @RequestParam(required = false) String error) {
        var lista = currentUser.establecimientos();
        String code = establecimiento != null && !establecimiento.isBlank()
                ? establecimiento
                : (lista.isEmpty() ? null : lista.get(0).getCode());
        String mesStr = mes != null && !mes.isBlank() ? mes : YearMonth.now().toString();
        model.addAttribute("establecimientos", lista);
        model.addAttribute("establecimiento", code == null ? "" : code);
        model.addAttribute("mes", mesStr);
        model.addAttribute("error", error);
        if (code == null) {
            return "planilla";
        }
        if (!currentUser.puedeAcceder(code)) {
            model.addAttribute("error", "No tenes acceso a ese establecimiento.");
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
            response.sendRedirect(volver + "&error=" + URLEncoder.encode("No tenes acceso a ese establecimiento.",
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

    @PostMapping("/planilla/cerrar")
    public String cerrar(@RequestParam String establecimiento, @RequestParam String mes) {
        return accion(establecimiento, mes, () -> {
            if (!currentUser.puedeAcceder(establecimiento)) {
                throw new DomainException("No tenes acceso a ese establecimiento.");
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
