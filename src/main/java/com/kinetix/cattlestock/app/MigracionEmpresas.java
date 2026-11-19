package com.kinetix.cattlestock.app;

import java.util.Comparator;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kinetix.cattlestock.data.CompanyEntity;
import com.kinetix.cattlestock.data.CompanyRepository;
import com.kinetix.cattlestock.data.EstablishmentEntity;
import com.kinetix.cattlestock.data.EstablishmentRepository;
import com.kinetix.cattlestock.data.UserEntity;
import com.kinetix.cattlestock.data.UserRepository;

/**
 * Pasa una base de la version de una sola empresa a la multiempresa: la empresa que ya existia se llama
 * "Prueba", los establecimientos sin empresa quedan en ella, y el administrador de antes, que lo veia todo,
 * pasa a superadmin. Es idempotente: en una base ya migrada no cambia nada.
 */
@Component
@Order(0)
public class MigracionEmpresas implements ApplicationRunner {

    static final String NOMBRE_ANTERIOR = "Ganadera de prueba";
    static final String NOMBRE_NUEVO = "Prueba";

    private final CompanyRepository companies;
    private final EstablishmentRepository establishments;
    private final UserRepository users;

    public MigracionEmpresas(CompanyRepository companies, EstablishmentRepository establishments,
                             UserRepository users) {
        this.companies = companies;
        this.establishments = establishments;
        this.users = users;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<CompanyEntity> todas = companies.findAll();
        if (todas.isEmpty()) return;

        if (todas.size() == 1 && NOMBRE_ANTERIOR.equals(todas.get(0).getName())) {
            todas.get(0).setName(NOMBRE_NUEVO);
            companies.save(todas.get(0));
        }
        Long primera = todas.stream().map(CompanyEntity::getId).min(Comparator.naturalOrder()).orElseThrow();

        for (EstablishmentEntity e : establishments.findByCompanyIdIsNull()) {
            e.setCompanyId(primera);
            establishments.save(e);
        }
        for (UserEntity u : users.findAll()) {
            if (u.getCompanyId() != null || u.getRole() == UserEntity.Role.SUPERADMIN) continue;
            if (u.getRole() == UserEntity.Role.ADMIN) {
                u.setRole(UserEntity.Role.SUPERADMIN); // el administrador de antes lo veia todo
            } else {
                u.setCompanyId(u.getEstablishments().stream().map(EstablishmentEntity::getCompanyId)
                        .filter(java.util.Objects::nonNull).findFirst().orElse(primera));
            }
            users.save(u);
        }
    }
}
