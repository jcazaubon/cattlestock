package com.kinetix.cattlestock.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kinetix.cattlestock.app.AdminService;
import com.kinetix.cattlestock.app.UserService;
import com.kinetix.cattlestock.data.CompanyEntity;
import com.kinetix.cattlestock.data.CompanyRepository;
import com.kinetix.cattlestock.data.EstablishmentRepository;
import com.kinetix.cattlestock.data.UserEntity;
import com.kinetix.cattlestock.data.UserRepository;
import com.kinetix.cattlestock.domain.DomainException;

/** Panel del superadmin: empresas, sus administradores y otros superadmins. */
@Controller
public class AdminController {

    /** Una empresa con sus cantidades, para la tabla del panel. */
    public record FilaEmpresa(CompanyEntity empresa, long establecimientos, long usuarios, boolean activa) {
    }

    private final AdminService admin;
    private final UserService userService;
    private final CompanyRepository companies;
    private final EstablishmentRepository establishments;
    private final UserRepository users;
    private final CurrentUser currentUser;

    public AdminController(AdminService admin, UserService userService, CompanyRepository companies,
                           EstablishmentRepository establishments, UserRepository users, CurrentUser currentUser) {
        this.admin = admin;
        this.userService = userService;
        this.companies = companies;
        this.establishments = establishments;
        this.users = users;
        this.currentUser = currentUser;
    }

    @GetMapping("/admin")
    public String panel(Model model, @RequestParam(required = false) String error) {
        Long activa = currentUser.empresaId().orElse(null);
        List<FilaEmpresa> filas = companies.findAllByOrderByNameAsc().stream()
                .map(c -> new FilaEmpresa(c, establishments.countByCompanyId(c.getId()),
                        users.countByCompanyId(c.getId()), c.getId().equals(activa)))
                .toList();
        model.addAttribute("error", error);
        model.addAttribute("empresas", filas);
        model.addAttribute("superadmins", users.findAllByOrderByUsernameAsc().stream()
                .filter(u -> u.getRole() == UserEntity.Role.SUPERADMIN).toList());
        return "admin";
    }

    @PostMapping("/admin/empresas")
    public String crearEmpresa(@RequestParam String nombre, @RequestParam String mesInicial,
                               @RequestParam String adminUsuario, @RequestParam String adminPassword) {
        try {
            admin.altaEmpresa(texto(nombre, "Escribi el nombre de la empresa"),
                    YearMonth.parse(texto(mesInicial, "Elegí el mes inicial")),
                    texto(adminUsuario, "Escribi el usuario del administrador"), adminPassword);
        } catch (DomainException | IllegalArgumentException | DateTimeParseException e) {
            return conError(e.getMessage());
        }
        return "redirect:/admin";
    }

    /** El superadmin pasa a trabajar en esa empresa. */
    @PostMapping("/admin/empresas/{id}/entrar")
    public String entrar(@PathVariable Long id) {
        currentUser.elegirEmpresa(id);
        return "redirect:/";
    }

    @PostMapping("/admin/superadmins")
    public String crearSuperadmin(@RequestParam String username, @RequestParam String password) {
        try {
            userService.crearSuperadmin(texto(username, "Escribi el usuario"), password);
        } catch (DomainException | IllegalArgumentException e) {
            return conError(e.getMessage());
        }
        return "redirect:/admin";
    }

    private String conError(String mensaje) {
        return "redirect:/admin?error=" + URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
    }

    private String texto(String valor, String mensajeSiFalta) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(mensajeSiFalta);
        return valor.trim();
    }
}
