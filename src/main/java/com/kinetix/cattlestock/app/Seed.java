package com.kinetix.cattlestock.app;

import java.time.YearMonth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.kinetix.cattlestock.data.CompanyEntity;
import com.kinetix.cattlestock.data.CompanyRepository;
import com.kinetix.cattlestock.domain.Species;

/**
 * Datos de prueba para arrancar la app mientras no haya pantallas de configuracion.
 * Corre una sola vez: si la empresa ya esta configurada, no hace nada. Provisional.
 */
@Component
@ConditionalOnProperty(name = "cattlestock.seed", havingValue = "true", matchIfMissing = true)
public class Seed implements ApplicationRunner {

    private final LedgerService service;
    private final CompanyRepository companies;

    public Seed(LedgerService service, CompanyRepository companies) {
        this.service = service;
        this.companies = companies;
    }

    @Override
    public void run(ApplicationArguments args) {
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
