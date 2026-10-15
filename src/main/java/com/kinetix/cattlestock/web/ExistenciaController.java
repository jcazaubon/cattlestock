package com.kinetix.cattlestock.web;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kinetix.cattlestock.app.LedgerService;
import com.kinetix.cattlestock.data.EstablishmentRepository;
import com.kinetix.cattlestock.domain.DomainException;
import com.kinetix.cattlestock.domain.Species;

@Controller
public class ExistenciaController {

    private final LedgerService service;
    private final EstablishmentRepository establishments;

    public ExistenciaController(LedgerService service, EstablishmentRepository establishments) {
        this.service = service;
        this.establishments = establishments;
    }

    @GetMapping("/existencia")
    public String existencia(Model model,
                              @RequestParam(required = false) String establecimiento,
                              @RequestParam(required = false) String especie,
                              @RequestParam(required = false) String categoria,
                              @RequestParam(required = false) String fecha,
                              @RequestParam(required = false) String error) {
        String fechaStr = s(fecha).isBlank() ? LocalDate.now().toString() : fecha;
        model.addAttribute("establecimientos", establishments.findAllByOrderByCodeAsc());
        model.addAttribute("establecimiento", s(establecimiento));
        model.addAttribute("especie", s(especie));
        model.addAttribute("categoria", s(categoria));
        model.addAttribute("fecha", fechaStr);
        model.addAttribute("error", error);
        try {
            Species sp = especie == null || especie.isBlank() ? null : Species.valueOf(especie);
            String est = establecimiento == null || establecimiento.isBlank() ? null : establecimiento;
            String cat = categoria == null || categoria.isBlank() ? null : categoria;
            model.addAttribute("filas", service.existencia(LocalDate.parse(fechaStr), est, sp, cat));
        } catch (DomainException | IllegalArgumentException | DateTimeParseException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "existencia";
    }

    private static String s(String valor) {
        return valor == null ? "" : valor;
    }
}
