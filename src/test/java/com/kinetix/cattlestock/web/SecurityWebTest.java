package com.kinetix.cattlestock.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.kinetix.cattlestock.app.LedgerService;
import com.kinetix.cattlestock.app.UserService;
import com.kinetix.cattlestock.data.UserEntity;
import com.kinetix.cattlestock.domain.Species;

/** Reglas de acceso: sesion, establecimientos asignados y reapertura solo para admin. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityWebTest {

    static final YearMonth SEP = YearMonth.of(2026, 9);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    LedgerService service;

    @Autowired
    UserService userService;

    void configurar() {
        service.configureCompany("Empresa de prueba", SEP);
        service.createEstablishment("P", "Principal");
        service.createCategory("P", "toros", "Toros", Species.VACUNOS);
        service.createEstablishment("LM", "La Marta");
        service.createCategory("LM", "vacas", "Vacas", Species.VACUNOS);
        service.setOpening("P", "toros", 51);
        userService.crear("pepe", "pepe", UserEntity.Role.CARGA, List.of("P"));
        userService.crear("admin", "admin", UserEntity.Role.ADMIN, List.of());
    }

    @Test
    void sinSesionMandaAlLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));
    }

    @Test
    void elUsuarioDeCargaSoloVeSusEstablecimientos() throws Exception {
        configurar();
        mockMvc.perform(get("/").with(user("pepe").roles("CARGA")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Principal")))
                .andExpect(content().string(not(containsString("La Marta"))));
    }

    @Test
    void elUsuarioDeCargaNoEntraALaConfiguracion() throws Exception {
        configurar();
        mockMvc.perform(get("/configuracion").with(user("pepe").roles("CARGA")))
                .andExpect(status().isForbidden());
    }

    @Test
    void elUsuarioDeCargaNoPuedeCargarEnUnEstablecimientoAjeno() throws Exception {
        configurar();
        mockMvc.perform(post("/movimientos").with(user("pepe").roles("CARGA")).with(csrf())
                        .param("fecha", "2026-09-03")
                        .param("establecimiento", "LM")
                        .param("categoria0", "vacas")
                        .param("tipo0", "COMPRA")
                        .param("cantidad0", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No podes cargar en LM")));
    }

    @Test
    void elUsuarioDeCargaNoPuedeReabrirUnaPlanilla() throws Exception {
        configurar();
        service.closeMonth("P", SEP);
        mockMvc.perform(post("/planilla/reabrir").with(user("pepe").roles("CARGA")).with(csrf())
                        .param("establecimiento", "P")
                        .param("mes", "2026-09"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("error")));
        assertTrue(service.planilla("P", SEP).closed(), "sigue cerrada");
    }

    @Test
    void elAdministradorPuedeReabrirUnaPlanilla() throws Exception {
        configurar();
        service.closeMonth("P", SEP);
        mockMvc.perform(post("/planilla/reabrir").with(user("admin").roles("ADMIN")).with(csrf())
                        .param("establecimiento", "P")
                        .param("mes", "2026-09"))
                .andExpect(status().is3xxRedirection());
        assertFalse(service.planilla("P", SEP).closed(), "queda abierta");
    }

    @Test
    void elLoginAceptaCredencialesCorrectas() throws Exception {
        configurar();
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "pepe")
                        .param("password", "pepe"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", not(containsString("error"))));
    }

    @Test
    void elLoginRechazaCredencialesIncorrectas() throws Exception {
        configurar();
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "pepe")
                        .param("password", "mal"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login?error")));
    }
}
