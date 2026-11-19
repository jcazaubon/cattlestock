package com.kinetix.cattlestock.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.kinetix.cattlestock.app.LedgerService;
import com.kinetix.cattlestock.app.UserService;
import com.kinetix.cattlestock.data.UserEntity;
import com.kinetix.cattlestock.domain.Species;

/** Las pantallas de carga no arrancan con el establecimiento vacio, y el panel de disponibles no empuja la linea. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CargaPorDefectoTest {

    @Autowired MockMvc mockMvc;
    @Autowired LedgerService service;
    @Autowired UserService userService;

    Long empresa;

    void configurar(boolean dosEstablecimientos) {
        empresa = service.createCompany("Ganadera Norte", YearMonth.of(2026, 9));
        service.createEstablishment(empresa, "N1", "Campo Norte");
        if (dosEstablecimientos) service.createEstablishment(empresa, "N2", "Campo Sur");
        service.createCategory(empresa, "toros", "Toros", Species.VACUNOS);
        service.setOpening("N1", "toros", 10);
        userService.crear("adminnorte", "x", UserEntity.Role.ADMIN, empresa, List.of());
    }

    org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
        return user("adminnorte").roles("ADMIN");
    }

    @Test
    void conUnSoloEstablecimientoLaCargaYaLoTraElegido() throws Exception {
        configurar(false);
        for (String ruta : List.of("/alta", "/grilla")) {
            mockMvc.perform(get(ruta).with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("<option value=\"N1\" selected>Campo Norte</option>")));
        }
    }

    @Test
    void conVariosArrancaVacioHastaQueCargaEnUnoYDespuesRecuerdaElUltimo() throws Exception {
        configurar(true);
        MockHttpSession sesion = new MockHttpSession();
        mockMvc.perform(get("/alta").session(sesion).with(admin()))
                .andExpect(content().string(not(containsString("selected>Campo"))));
        // carga un lote en el segundo establecimiento
        mockMvc.perform(post("/grilla").session(sesion).with(admin()).with(csrf())
                        .param("establecimiento", "N2").param("fecha", "2026-09-10").param("especie", "VACUNOS")
                        .param("venta_toros", "0").param("nacimiento_toros", "3"))
                .andExpect(status().is3xxRedirection());
        // la proxima carga, en esta sesion, arranca en el
        for (String ruta : List.of("/alta", "/grilla")) {
            mockMvc.perform(get(ruta).session(sesion).with(admin()))
                    .andExpect(content().string(containsString("<option value=\"N2\" selected>Campo Sur</option>")));
        }
        // y otra sesion sigue empezando vacia
        mockMvc.perform(get("/alta").with(admin()))
                .andExpect(content().string(not(containsString("selected>Campo"))));
    }

    @Test
    void elPanelDeDisponiblesViene_cerradoParaQueLaLineaDeCargaQuedeALaVista() throws Exception {
        configurar(false);
        mockMvc.perform(get("/alta").with(admin()))
                .andExpect(content().string(containsString("<details class=\"disponible\">")))
                .andExpect(content().string(not(containsString("<details class=\"disponible\" open>"))));
    }
}
