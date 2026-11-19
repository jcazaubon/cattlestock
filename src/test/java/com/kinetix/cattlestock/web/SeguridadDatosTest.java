package com.kinetix.cattlestock.web;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.kinetix.cattlestock.app.LedgerService;
import com.kinetix.cattlestock.app.UserService;
import com.kinetix.cattlestock.data.UserEntity;
import com.kinetix.cattlestock.domain.Species;

/** Una empresa no puede tocar los datos de otra: ni siquiera con un codigo de establecimiento ajeno. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeguridadDatosTest {

    static final YearMonth SEP = YearMonth.of(2026, 9);

    @Autowired MockMvc mockMvc;
    @Autowired LedgerService service;
    @Autowired UserService userService;

    void configurar() {
        Long norte = service.createCompany("Ganadera Norte", SEP);
        service.createEstablishment(norte, "N1", "Campo Norte");
        service.createCategory(norte, "toros", "Toros", Species.VACUNOS);
        service.setOpening("N1", "toros", 10);
        Long sur = service.createCompany("Ganadera Sur", SEP);
        service.createEstablishment(sur, "S1", "Campo Sur");
        service.createCategory(sur, "vacas", "Vacas", Species.VACUNOS);
        service.setOpening("S1", "vacas", 77);
        userService.crear("adminnorte", "x", UserEntity.Role.ADMIN, norte, List.of());
    }

    @Test
    void unAdministradorNoPuedeReabrirLaPlanillaDeOtraEmpresa() throws Exception {
        configurar();
        service.closeMonth("S1", SEP);
        mockMvc.perform(post("/planilla/reabrir").with(user("adminnorte").roles("ADMIN")).with(csrf())
                .param("establecimiento", "S1").param("mes", SEP.toString()));
        assertTrue(service.planilla("S1", SEP).closed(), "la planilla de S1 sigue cerrada");
    }
}
