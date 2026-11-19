package com.kinetix.cattlestock.app;

import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kinetix.cattlestock.data.UserEntity;

/** Operaciones del superadmin que tocan mas de una cosa a la vez. */
@Service
public class AdminService {

    private final LedgerService ledger;
    private final UserService users;

    public AdminService(LedgerService ledger, UserService users) {
        this.ledger = ledger;
        this.users = users;
    }

    /** Da de alta una empresa junto con su primer administrador: si algo falla, no queda ninguna de las dos. */
    @Transactional
    public Long altaEmpresa(String nombre, YearMonth mesInicial, String adminUsuario, String adminPassword) {
        Long id = ledger.createCompany(nombre, mesInicial);
        users.crearConClaveTemporal(adminUsuario, adminPassword, UserEntity.Role.ADMIN, id, List.of());
        return id;
    }
}
