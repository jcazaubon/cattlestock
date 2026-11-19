package com.kinetix.cattlestock.web;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
import com.kinetix.cattlestock.data.UserRepository;
import com.kinetix.cattlestock.domain.DomainException;
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
    @Autowired UserRepository users;

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

    // ---- claves de los usuarios ----

    @Test
    void laClaveQueDaElAdministradorDebeCumplirLaPoliticaYSeCambiaEnElPrimerIngreso() throws Exception {
        configurar();
        Long norte = service.companyOf("N1");
        assertThrows(DomainException.class, () -> userService.crearConClaveTemporal("pepe", "corta", UserEntity.Role.CARGA, norte, List.of("N1")));
        assertThrows(DomainException.class, () -> userService.crearConClaveTemporal("pepepepe", "PEPEPEPE", UserEntity.Role.CARGA, norte, List.of("N1")));
        userService.crearConClaveTemporal("pepe", "temporal-123", UserEntity.Role.CARGA, norte, List.of("N1"));
        assertTrue(users.findByUsername("pepe").orElseThrow().isMustChangePassword());

        // mientras no la cambie, solo puede ir a cambiarla
        mockMvc.perform(get("/planilla").with(user("pepe").roles("CARGA")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/cuenta/clave")));
        mockMvc.perform(get("/cuenta/clave").with(user("pepe").roles("CARGA")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("te dio el administrador")));

        // clave actual incorrecta, nueva corta o repetida distinta: no se cambia
        for (String[] p : new String[][] {{"mala", "nueva-clave-1", "nueva-clave-1"}, {"temporal-123", "corta", "corta"},
                {"temporal-123", "nueva-clave-1", "otra-distinta"}, {"temporal-123", "temporal-123", "temporal-123"}}) {
            mockMvc.perform(post("/cuenta/clave").with(user("pepe").roles("CARGA")).with(csrf())
                    .param("actual", p[0]).param("nueva", p[1]).param("repetir", p[2]))
                    .andExpect(status().isOk());
            assertTrue(users.findByUsername("pepe").orElseThrow().isMustChangePassword());
        }

        mockMvc.perform(post("/cuenta/clave").with(user("pepe").roles("CARGA")).with(csrf())
                .param("actual", "temporal-123").param("nueva", "nueva-clave-1").param("repetir", "nueva-clave-1"))
                .andExpect(status().is3xxRedirection());
        assertFalse(users.findByUsername("pepe").orElseThrow().isMustChangePassword());
        mockMvc.perform(get("/planilla").with(user("pepe").roles("CARGA")))
                .andExpect(status().isOk());
    }

    @Test
    void despuesDeVariosIntentosFallidosSeBloqueaElLoginAunqueLaClaveSeaCorrecta() throws Exception {
        configurar();
        userService.crear("lola", "clave-correcta", UserEntity.Role.ADMIN, service.companyOf("N1"), List.of());
        org.springframework.test.web.servlet.request.RequestPostProcessor desde = r -> {
            r.setRemoteAddr("10.9.9.9");
            return r;
        };
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/login").with(desde).with(csrf()).param("username", "lola").param("password", "mala"))
                    .andExpect(header().string("Location", containsString("/login?error")));
        }
        mockMvc.perform(post("/login").with(desde).with(csrf()).param("username", "lola").param("password", "clave-correcta"))
                .andExpect(header().string("Location", containsString("/login?bloqueado")));
        mockMvc.perform(get("/login").param("bloqueado", ""))
                .andExpect(content().string(containsString("Demasiados intentos fallidos")));
    }

    @Test
    void laPoliticaDeContenidoNoPermiteScriptsDeOtroOrigen() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("object-src 'none'")))
                .andExpect(header().string("Referrer-Policy", containsString("strict-origin-when-cross-origin")));
    }
}
