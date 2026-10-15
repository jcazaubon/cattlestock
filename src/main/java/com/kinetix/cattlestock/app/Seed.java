package com.kinetix.cattlestock.app;

import java.time.YearMonth;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.kinetix.cattlestock.app.LedgerService;
import com.kinetix.cattlestock.app.UserService;
import com.kinetix.cattlestock.data.CompanyEntity;
import com.kinetix.cattlestock.data.CompanyRepository;
import com.kinetix.cattlestock.data.UserEntity;
import com.kinetix.cattlestock.data.UserRepository;
import com.kinetix.cattlestock.domain.Species;

/**
 * Datos de prueba para arrancar la app: empresa, establecimientos, stock inicial y el
 * usuario admin (admin/admin). Corre una sola vez, si la empresa no esta configurada.
 */
@Component
@ConditionalOnProperty(name = "cattlestock.seed", havingValue = "true", matchIfMissing = true)
public class Seed implements ApplicationRunner {

    private final LedgerService service;
    private final UserService userService;
    private final CompanyRepository companies;
    private final UserRepository users;

    public Seed(LedgerService service, UserService userService, CompanyRepository companies, UserRepository users) {
        this.service = service;
        this.userService = userService;
        this.companies = companies;
        this.users = users;
    }

    @Override
    public void run(ApplicationArguments args) {
        // El usuario admin se garantiza siempre: si la base venia de una version
        // sin usuarios, hay que poder entrar.
        if (users.count() == 0) {
            userService.crear("admin", "admin", UserEntity.Role.ADMIN, List.of());
        }
        if (companies.findById(CompanyEntity.ID).isPresent()) {
            return;
        }
        YearMonth inicio = YearMonth.now();
        service.configureCompany("Ganadera de prueba", inicio);
        service.createEstablishment("P", "Principal");
        service.createCategory("P", "toros", "Toros", Species.VACUNOS);
        service.createCategory("P", "vplantel", "Vacas Plantel", Species.VACUNOS);
        service.createCategory("P", "vgeneral", "Vacas General", Species.VACUNOS);
        service.createCategory("P", "vaqplantel", "Vaquillonas Cria Plantel", Species.VACUNOS);
        service.createCategory("P", "orejanos", "Terneros Orejanos", Species.VACUNOS);
        service.createCategory("P", "vengorde", "Vacas en Engorde", Species.VACUNOS);
        service.createCategory("P", "toroseng", "Toros Engorde", Species.VACUNOS);
        service.createCategory("P", "yeguarizos", "Yeguarizos", Species.YEGUARIZOS);
        service.createEstablishment("LM", "La Marta");
        service.createCategory("LM", "vacas", "Vacas", Species.VACUNOS);

        service.setOpening("P", "toros", 51);
        service.setOpening("P", "vplantel", 37);
        service.setOpening("P", "vgeneral", 658);
        service.setOpening("P", "vaqplantel", 22);
        service.setOpening("P", "orejanos", 818);
        service.setOpening("P", "vengorde", 3);
        service.setOpening("P", "toroseng", 0);
        service.setOpening("P", "yeguarizos", 58);
        service.setOpening("LM", "vacas", 5);
    }
}
