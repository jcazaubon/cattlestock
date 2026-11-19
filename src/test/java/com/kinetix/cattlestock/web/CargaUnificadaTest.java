package com.kinetix.cattlestock.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

/** Cargar movimientos es una sola entrada con dos formas: por planilla y por línea. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CargaUnificadaTest {

    @Autowired MockMvc mockMvc;
    @Autowired LedgerService service;
    @Autowired UserService userService;

    void configurar() {
        Long empresa = service.createCompany("Ganadera Norte", YearMonth.of(2026, 9));
        service.createEstablishment(empresa, "N1", "Campo Norte");
        userService.crear("adminnorte", "x", UserEntity.Role.ADMIN, empresa, List.of());
    }

    @Test
    void hayUnSoloMenuYAmbasPantallasMuestranLasPestanas() throws Exception {
        configurar();
        for (String ruta : List.of("/alta", "/grilla")) {
            mockMvc.perform(get(ruta).with(user("adminnorte").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("<h1>Cargar movimientos</h1>")))
                    .andExpect(content().string(containsString(">Cargar movimientos</a>")))
                    .andExpect(content().string(not(containsString(">Carga rápida<"))))
                    .andExpect(content().string(containsString(">Por planilla</a>")))
                    .andExpect(content().string(containsString(">Por línea</a>")));
        }
        mockMvc.perform(get("/alta").with(user("adminnorte").roles("ADMIN")))
                .andExpect(content().string(containsString("href=\"/alta\" aria-current=\"page\">Por línea")));
        mockMvc.perform(get("/grilla").with(user("adminnorte").roles("ADMIN")))
                .andExpect(content().string(containsString("href=\"/grilla\" aria-current=\"page\">Por planilla")));
    }
}
