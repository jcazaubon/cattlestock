package com.kinetix.cattlestock.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.kinetix.cattlestock.domain.DiasHabiles;
import com.kinetix.cattlestock.domain.Species;

/** El periodo es un mes y la fecha de la carga arranca en su ultimo dia habil, pero se puede cambiar. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PeriodoYFechaTest {

    @Autowired MockMvc mockMvc;
    @Autowired LedgerService service;
    @Autowired UserService userService;

    void configurar() {
        Long empresa = service.createCompany("Ganadera Norte", YearMonth.of(2026, 9));
        service.createEstablishment(empresa, "N1", "Campo Norte");
        service.createCategory(empresa, "toros", "Toros", Species.VACUNOS);
        userService.crear("adminnorte", "x", UserEntity.Role.ADMIN, empresa, List.of());
    }

    org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
        return user("adminnorte").roles("ADMIN");
    }

    @Test
    void elAltaAbreEnElMesActualConSuUltimoDiaHabilYUnCampoDePeriodo() throws Exception {
        configurar();
        String esperado = DiasHabiles.porDefecto(YearMonth.now(), java.time.LocalDate.now()).toString();
        mockMvc.perform(get("/alta").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<input type=\"month\" name=\"periodo\" value=\"" + YearMonth.now() + "\"")))
                .andExpect(content().string(containsString("id=\"fecha\" name=\"fecha\" value=\"" + esperado + "\"")))
                // cambiar el periodo pide la fecha nueva y el resto de la pantalla se entera
                .andExpect(content().string(containsString("hx-get=\"/campo-fecha\"")))
                .andExpect(content().string(containsString("fecha-actualizada from:body")));
    }

    @Test
    void siSePideUnPeriodoLaFechaEsSuUltimoDiaHabil() throws Exception {
        configurar();
        mockMvc.perform(get("/alta").param("periodo", "2026-10").with(admin()))
                .andExpect(content().string(containsString("name=\"periodo\" value=\"2026-10\"")))
                .andExpect(content().string(containsString("name=\"fecha\" value=\"2026-10-30\""))); // el 31 es sabado
    }

    @Test
    void unaFechaYaEscritaSeRespetaYDefineElPeriodo() throws Exception {
        configurar();
        // al volver de un error, o al elegir otra fecha, no se pisa: queda la que habia y su mes como periodo
        mockMvc.perform(get("/alta").param("fecha", "2026-09-15").with(admin()))
                .andExpect(content().string(containsString("name=\"fecha\" value=\"2026-09-15\"")))
                .andExpect(content().string(containsString("name=\"periodo\" value=\"2026-09\"")))
                .andExpect(content().string(not(containsString("value=\"2026-09-30\""))));
    }

    @Test
    void elCampoDeFechaSeRecalculaAlCambiarElPeriodoYAvisa() throws Exception {
        configurar();
        mockMvc.perform(get("/campo-fecha").param("periodo", "2027-01").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"fecha\" value=\"2027-01-29\""))) // el 31 es domingo
                .andExpect(content().string(containsString("id=\"fecha\"")))
                .andExpect(header().string("HX-Trigger", "fecha-actualizada"));
        // un periodo que no es un mes valido cae en el mes actual, sin romper la pantalla
        String actual = DiasHabiles.porDefecto(YearMonth.now(), java.time.LocalDate.now()).toString();
        mockMvc.perform(get("/campo-fecha").param("periodo", "esto-no-es-un-mes").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"" + actual + "\"")));
        mockMvc.perform(get("/campo-fecha").with(admin()))
                .andExpect(content().string(containsString("value=\"" + actual + "\"")));
    }

    @Test
    void laCargaRapidaTieneElMismoPeriodoYLaMismaFechaPorDefecto() throws Exception {
        configurar();
        mockMvc.perform(get("/grilla").param("establecimiento", "N1").param("periodo", "2026-11").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"periodo\" value=\"2026-11\"")))
                .andExpect(content().string(containsString("name=\"fecha\" value=\"" + DiasHabiles.porDefecto(java.time.YearMonth.of(2026, 11), java.time.LocalDate.now()) + "\"")));
        mockMvc.perform(get("/grilla").param("establecimiento", "N1").param("fecha", "2026-10-12").with(admin()))
                .andExpect(content().string(containsString("name=\"fecha\" value=\"2026-10-12\"")))
                .andExpect(content().string(containsString("name=\"periodo\" value=\"2026-10\"")));
    }

    @Test
    void elCampoDeFechaSigueSiendoObligatorioYEditable() throws Exception {
        configurar();
        mockMvc.perform(get("/campo-fecha").param("periodo", "2026-11").with(admin()))
                .andExpect(content().string(containsString("type=\"date\"")))
                .andExpect(content().string(containsString("required")))
                .andExpect(content().string(not(containsString("readonly"))))
                .andExpect(content().string(not(containsString("disabled"))));
    }
}
