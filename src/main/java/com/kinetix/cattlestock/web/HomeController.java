package com.kinetix.cattlestock.web;

import java.time.YearMonth;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final CurrentUser currentUser;

    public HomeController(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("appName", "CattleStock");
        model.addAttribute("establecimientos", currentUser.establecimientos());
        model.addAttribute("mesActual", YearMonth.now().toString());
        model.addAttribute("esAdmin", currentUser.esAdmin());
        return "index";
    }
}
