package com.kinetix.cattlestock.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kinetix.cattlestock.app.LedgerService;
import com.kinetix.cattlestock.data.EstablishmentEntity;
import com.kinetix.cattlestock.data.EstablishmentRepository;
import com.kinetix.cattlestock.domain.DomainException;

@Controller
public class PlanillaController {

    private final LedgerService service;
    private final EstablishmentRepository establishments;

    public PlanillaController(LedgerService service, EstablishmentRepository establishments) {
        this.service = service;
        this.establishments = establishments;
    }

    @GetMapping("/planilla")
    public String planilla(Model model,
                           @RequestParam(required = false) String establecimiento,
                           @RequestParam(required = false) String mes,
                           @RequestParam(required = false) String error) {
        List<EstablishmentEntity> lista = establishments.findAllByOrderByCodeAsc();
        String code = establecimiento != null && !establecimiento.isBlank()
                ? establecimiento
                : (lista.isEmpty() ? null : lista.get(0).getCode());
        String mesStr = mes != null && !mes.isBlank() ? mes : YearMonth.now().toString();
        model.addAttribute("establecimientos", lista);
        model.addAttribute("establecimiento", code == null ? "" : code);
        model.addAttribute("mes", mesStr);
        model.addAttribute("error", error);
        if (code != null) {
            try {
                model.addAttribute("planilla", service.planilla(code, YearMonth.parse(mesStr)));
            } catch (DomainException | DateTimeParseException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "planilla";
    }

    @PostMapping("/planilla/cerrar")
    public String cerrar(@RequestParam String establecimiento, @RequestParam String mes) {
        return accion(establecimiento, mes, () -> service.closeMonth(establecimiento, YearMonth.parse(mes)));
    }

    @PostMapping("/planilla/reabrir")
    public String reabrir(@RequestParam String establecimiento, @RequestParam String mes) {
        return accion(establecimiento, mes, () -> service.reopenLastMonth(establecimiento));
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
