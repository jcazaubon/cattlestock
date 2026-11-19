package com.kinetix.cattlestock.app;

import java.time.YearMonth;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.kinetix.cattlestock.app.LedgerService;
import com.kinetix.cattlestock.app.UserService;
import com.kinetix.cattlestock.data.CompanyRepository;
import com.kinetix.cattlestock.data.UserEntity;
import com.kinetix.cattlestock.data.UserRepository;
import com.kinetix.cattlestock.domain.Species;

/**
 * Datos de prueba para arrancar la app: la empresa Prueba, sus establecimientos, el stock inicial y el
 * superadmin (admin/admin). Corre una sola vez, si la empresa no esta configurada.
 */
@Component
@Order(10)
@ConditionalOnProperty(name = "cattlestock.seed", havingValue = "true", matchIfMissing = true)
public class Seed implements ApplicationRunner {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Seed.class);

    /** La clave del superadmin inicial. En produccion se define por variable de entorno; nunca queda en el codigo. */
    @org.springframework.beans.factory.annotation.Value("${cattlestock.admin-password:admin}")
    private String adminPassword;

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
            userService.crearSuperadmin("admin", adminPassword);
            if ("admin".equals(adminPassword)) {
                LOG.warn("Se creo el superadmin 'admin' con la clave por defecto. Antes de publicar el sistema "
                        + "definir cattlestock.admin-password (variable CATTLESTOCK_ADMIN_PASSWORD) o cambiarla.");
            }
        }
        if (!companies.findAll().isEmpty()) {
            return;
        }
        YearMonth inicio = YearMonth.now();
        Long empresa = service.createCompany("Prueba", inicio);
        service.createEstablishment(empresa, "P", "Principal");
        service.createCategory(empresa, "toros", "Toros", Species.VACUNOS);
        service.createCategory(empresa, "vplantel", "Vacas Plantel", Species.VACUNOS);
        service.createCategory(empresa, "vgeneral", "Vacas General", Species.VACUNOS);
        service.createCategory(empresa, "vaqplantel", "Vaquillonas Cria Plantel", Species.VACUNOS);
        service.createCategory(empresa, "orejanos", "Terneros Orejanos", Species.VACUNOS);
        service.createCategory(empresa, "vengorde", "Vacas en Engorde", Species.VACUNOS);
        service.createCategory(empresa, "toroseng", "Toros Engorde", Species.VACUNOS);
        service.createCategory(empresa, "yeguarizos", "Yeguarizos", Species.YEGUARIZOS);
        service.createEstablishment(empresa, "LM", "La Marta");
        service.createCategory(empresa, "vacas", "Vacas", Species.VACUNOS);

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
