package com.kinetix.cattlestock.web;

import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.kinetix.cattlestock.data.EstablishmentEntity;
import com.kinetix.cattlestock.data.EstablishmentRepository;

@Controller
public class HomeController {

    private final EstablishmentRepository establishments;

    public HomeController(EstablishmentRepository establishments) {
        this.establishments = establishments;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<EstablishmentEntity> lista = establishments.findAllByOrderByCodeAsc();
        model.addAttribute("appName", "CattleStock");
        model.addAttribute("establecimientos", lista);
        model.addAttribute("mesActual", YearMonth.now().toString());
        return "index";
    }
}
