package com.kinetix.cattlestock.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.YearMonth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.kinetix.cattlestock.app.LedgerService;
import com.kinetix.cattlestock.data.MovementRepository;
import com.kinetix.cattlestock.domain.Species;

/** Pruebas de las pantallas de alta y planilla contra H2 en memoria. */
@SpringBootTest
@Transactional
class WebTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    LedgerService service;

    @Autowired
    MovementRepository movements;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    void configurar() {
        service.configureCompany("Empresa de prueba", YearMonth.of(2026, 9));
        service.createEstablishment("P", "Principal");
        service.createCategory("P", "toros", "Toros", Species.VACUNOS);
        service.createCategory("P", "toroseng", "Toros Engorde", Species.VACUNOS);
        service.setOpening("P", "toros", 51);
        service.setOpening("P", "toroseng", 0);
    }

    @Test
    void laPantallaDeAltaRenderiza() throws Exception {
        configurar();
        mockMvc.perform(get("/alta"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cargar movimiento")))
                .andExpect(content().string(containsString("Principal")));
    }

    @Test
    void unAltaValidaRedirigeALaPlanillaYPersiste() throws Exception {
        configurar();
        mockMvc.perform(post("/movimientos")
                        .param("fecha", "2026-09-03")
                        .param("tipo", "COMPRA")
                        .param("establecimiento", "P")
                        .param("categoria", "toros")
                        .param("cantidad", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planilla?establecimiento=P&mes=2026-09"));
        org.junit.jupiter.api.Assertions.assertEquals(1, movements.count());
    }

    @Test
    void unAltaRechazadaVuelveConErrorEnEspanolYNoPersiste() throws Exception {
        configurar();
        mockMvc.perform(post("/movimientos")
                        .param("fecha", "2026-09-03")
                        .param("tipo", "VENTA")
                        .param("establecimiento", "P")
                        .param("categoria", "toroseng")
                        .param("cantidad", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Stock insuficiente")))
                .andExpect(content().string(containsString("Cargar movimiento")));
        org.junit.jupiter.api.Assertions.assertEquals(0, movements.count());
    }

    @Test
    void laPlanillaRenderizaConLosDatosDelMes() throws Exception {
        configurar();
        mockMvc.perform(post("/movimientos")
                        .param("fecha", "2026-09-03")
                        .param("tipo", "COMPRA")
                        .param("establecimiento", "P")
                        .param("categoria", "toros")
                        .param("cantidad", "10"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/planilla").param("establecimiento", "P").param("mes", "2026-09"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Toros")))
                .andExpect(content().string(containsString("<strong>61</strong>")))
                .andExpect(content().string(containsString("Cerrar mes")));
    }

    @Test
    void laGrillaCargaUnLoteYSaltaALaPlanilla() throws Exception {
        configurar();
        mockMvc.perform(post("/grilla")
                        .param("establecimiento", "P")
                        .param("fecha", "2026-09-03")
                        .param("compra_toros", "10")
                        .param("nacimiento_toros", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planilla?establecimiento=P&mes=2026-09"));
        org.junit.jupiter.api.Assertions.assertEquals(2, movements.count());
    }

    @Test
    void laGrillaRechazadaMuestraElErrorConservaLosValoresYNoPersiste() throws Exception {
        configurar();
        mockMvc.perform(post("/grilla")
                        .param("establecimiento", "P")
                        .param("fecha", "2026-09-03")
                        .param("compra_toros", "10")
                        .param("venta_toroseng", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Stock insuficiente")))
                .andExpect(content().string(containsString("value=\"10\"")))
                .andExpect(content().string(containsString("Carga rapida en grilla")));
        org.junit.jupiter.api.Assertions.assertEquals(0, movements.count());
    }

    @Test
    void elFragmentoDeCategoriasRespondeSoloConElSelect() throws Exception {
        configurar();
        mockMvc.perform(get("/alta/categorias").param("establecimiento", "P"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Toros")))
                .andExpect(content().string(containsString("<select name=\"categoria\"")));
    }
}
