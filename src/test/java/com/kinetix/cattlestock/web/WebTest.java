package com.kinetix.cattlestock.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.kinetix.cattlestock.app.LedgerService;
import com.kinetix.cattlestock.app.UserService;
import com.kinetix.cattlestock.data.MovementRepository;
import com.kinetix.cattlestock.data.UserEntity;
import com.kinetix.cattlestock.domain.Species;

/** Pruebas de las pantallas, como administrador. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN")
class WebTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    LedgerService service;

    @Autowired
    UserService userService;

    @Autowired
    MovementRepository movements;

    void configurar() {
        service.configureCompany("Empresa de prueba", YearMonth.of(2026, 9));
        service.createEstablishment("P", "Principal");
        service.createCategory("P", "toros", "Toros", Species.VACUNOS);
        service.createCategory("P", "toroseng", "Toros Engorde", Species.VACUNOS);
        service.setOpening("P", "toros", 51);
        service.setOpening("P", "toroseng", 0);
        service.createEstablishment("LM", "La Marta");
        service.createCategory("LM", "vacas", "Vacas", Species.VACUNOS);
        service.setOpening("LM", "vacas", 5);
        userService.crear("admin", "admin", UserEntity.Role.ADMIN, java.util.List.of());
    }

    @Test
    void laPantallaDeAltaRenderizaConUnaLineaYBotonDeAgregar() throws Exception {
        configurar();
        mockMvc.perform(get("/alta"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cargar movimientos")))
                .andExpect(content().string(containsString("Principal")))
                .andExpect(content().string(containsString("Agregar linea")))
                .andExpect(content().string(containsString("name=\"categoria0\"")))
                .andExpect(content().string(containsString("Guardar lote")));
    }

    @Test
    void unAltaValidaRedirigeALaPlanillaYPersiste() throws Exception {
        configurar();
        mockMvc.perform(post("/movimientos").with(csrf())
                        .param("fecha", "2026-09-03")
                        .param("establecimiento", "P")
                        .param("categoria0", "toros")
                        .param("tipo0", "COMPRA")
                        .param("cantidad0", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planilla?establecimiento=P&mes=2026-09"));
        org.junit.jupiter.api.Assertions.assertEquals(1, movements.count());
    }

    @Test
    void unLoteDeLineasSeGuardaCompleto() throws Exception {
        configurar();
        mockMvc.perform(post("/movimientos").with(csrf())
                        .param("fecha", "2026-09-03")
                        .param("establecimiento", "P")
                        .param("categoria0", "toros")
                        .param("tipo0", "COMPRA")
                        .param("cantidad0", "10")
                        .param("categoria1", "toros")
                        .param("tipo1", "NACIMIENTO")
                        .param("cantidad1", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planilla?establecimiento=P&mes=2026-09"));
        org.junit.jupiter.api.Assertions.assertEquals(2, movements.count());
    }

    @Test
    void unLoteConUnaLineaInvalidaNoGuardaNadaYConservaLoEscrito() throws Exception {
        configurar();
        mockMvc.perform(post("/movimientos").with(csrf())
                        .param("fecha", "2026-09-03")
                        .param("establecimiento", "P")
                        .param("categoria0", "toros")
                        .param("tipo0", "COMPRA")
                        .param("cantidad0", "10")
                        .param("categoria1", "toroseng")
                        .param("tipo1", "VENTA")
                        .param("cantidad1", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Stock insuficiente")))
                .andExpect(content().string(containsString("value=\"10\"")))
                .andExpect(content().string(containsString("name=\"cantidad1\"")));
        org.junit.jupiter.api.Assertions.assertEquals(0, movements.count());
    }

    @Test
    void unAltaRechazadaVuelveConErrorEnEspanolYNoPersiste() throws Exception {
        configurar();
        mockMvc.perform(post("/movimientos").with(csrf())
                        .param("fecha", "2026-09-03")
                        .param("establecimiento", "P")
                        .param("categoria0", "toroseng")
                        .param("tipo0", "VENTA")
                        .param("cantidad0", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Stock insuficiente")))
                .andExpect(content().string(containsString("Cargar movimientos")));
        org.junit.jupiter.api.Assertions.assertEquals(0, movements.count());
    }

    @Test
    void laPlanillaRenderizaConLosDatosDelMes() throws Exception {
        configurar();
        mockMvc.perform(post("/movimientos").with(csrf())
                        .param("fecha", "2026-09-03")
                        .param("establecimiento", "P")
                        .param("categoria0", "toros")
                        .param("tipo0", "COMPRA")
                        .param("cantidad0", "10"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/planilla").param("establecimiento", "P").param("mes", "2026-09"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Toros")))
                .andExpect(content().string(containsString("<strong>61</strong>")))
                .andExpect(content().string(containsString("Cerrar mes")))
                .andExpect(content().string(containsString("Descargar Excel")));
    }

    @Test
    void laGrillaCargaUnLoteYSaltaALaPlanilla() throws Exception {
        configurar();
        mockMvc.perform(post("/grilla").with(csrf())
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
        mockMvc.perform(post("/grilla").with(csrf())
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
    void alElegirTrasladoElDestinoNuncaOfreceElOrigen() throws Exception {
        configurar();
        mockMvc.perform(get("/alta/campos-de-linea")
                        .param("establecimiento", "P")
                        .param("categoria0", "toros")
                        .param("tipo0", "TRASLADO"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"destinoEstablecimiento0\"")))
                .andExpect(content().string(containsString("La Marta")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("value=\"P\""))));
    }

    @Test
    void elBotonDeAgregarLineaTraeUnaLineaNuevaYElCambioDeEstablecimientoResetea() throws Exception {
        configurar();
        mockMvc.perform(get("/alta/linea")
                        .param("establecimiento", "P")
                        .param("tipo0", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"categoria1\"")))
                .andExpect(content().string(containsString("id=\"linea-1\"")));

        mockMvc.perform(get("/alta/lineas").param("establecimiento", "LM"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"categoria0\"")))
                .andExpect(content().string(containsString("Vacas")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("categoria1"))));
    }

    @Test
    void unTrasladoAlMismoEstablecimientoSeRechazaConMensaje() throws Exception {
        configurar();
        mockMvc.perform(post("/movimientos").with(csrf())
                        .param("fecha", "2026-09-03")
                        .param("establecimiento", "P")
                        .param("categoria0", "toros")
                        .param("tipo0", "TRASLADO")
                        .param("destinoEstablecimiento0", "P")
                        .param("destinoCategoria0", "toroseng")
                        .param("cantidad0", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El traslado debe ser a otro establecimiento")));
        org.junit.jupiter.api.Assertions.assertEquals(0, movements.count());
    }

    @Test
    void elFragmentoDeFilasDeLaGrillaRespondeYConservaLoEscrito() throws Exception {
        configurar();
        mockMvc.perform(get("/grilla/filas").param("establecimiento", "P"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"compra_toros\"")))
                .andExpect(content().string(containsString("name=\"consumo_toros\"")));

        mockMvc.perform(get("/grilla/filas")
                        .param("establecimiento", "P")
                        .param("compra_toros", "7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"7\"")));
    }

    @Test
    void laVistaGeneralMuestraLaExistenciaALaFecha() throws Exception {
        configurar();
        mockMvc.perform(get("/existencia").param("establecimiento", "P").param("fecha", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Toros")))
                .andExpect(content().string(containsString(">51<")));
    }

    @Test
    void laConfiguracionSeCompletaPorPantalla() throws Exception {
        userService.crear("admin", "admin", UserEntity.Role.ADMIN, java.util.List.of());
        mockMvc.perform(post("/configuracion/empresa").with(csrf())
                        .param("nombre", "Ganadera")
                        .param("mesInicial", "2026-09"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/configuracion/establecimientos").with(csrf())
                        .param("codigo", "X")
                        .param("nombre", "Campo Nuevo"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/configuracion/categorias").with(csrf())
                        .param("establecimiento", "X")
                        .param("codigo", "novillos")
                        .param("nombre", "Novillos")
                        .param("especie", "VACUNOS"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/configuracion/stock").with(csrf())
                        .param("establecimiento", "X")
                        .param("stock_novillos", "40"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/planilla").param("establecimiento", "X").param("mes", "2026-09"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Novillos")))
                .andExpect(content().string(containsString("<strong>40</strong>")));
    }

    @Test
    void laConfiguracionCreaUnUsuarioDeCarga() throws Exception {
        configurar();
        mockMvc.perform(post("/configuracion/usuarios").with(csrf())
                        .param("username", "pepe")
                        .param("password", "secreto")
                        .param("rol", "CARGA")
                        .param("establecimientos", "P"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/configuracion"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("pepe")));
    }

    @Test
    void mortandadConCuerosNoNumericosAvisaEnEspanolYNoPersiste() throws Exception {
        configurar();
        mockMvc.perform(post("/movimientos").with(csrf())
                        .param("fecha", "2026-09-03")
                        .param("establecimiento", "P")
                        .param("categoria0", "toros")
                        .param("tipo0", "MORTANDAD")
                        .param("cantidad0", "1")
                        .param("cueros0", "abc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Los cueros deben ser un numero entero")))
                .andExpect(content().string(containsString("Cargar movimientos")));
        org.junit.jupiter.api.Assertions.assertEquals(0, movements.count());
    }

    @Test
    void unEnvioSinFechaVuelveAlFormularioConAvisoYLoEscrito() throws Exception {
        mockMvc.perform(post("/movimientos").with(csrf())
                        .param("establecimiento", "P")
                        .param("categoria0", "toros")
                        .param("tipo0", "COMPRA")
                        .param("cantidad0", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Elegi la fecha")))
                .andExpect(content().string(containsString("value=\"3\"")));
    }

    @Test
    void unEnvioSinLineasAvisaYNoGuardaNada() throws Exception {
        configurar();
        mockMvc.perform(post("/movimientos").with(csrf())
                        .param("fecha", "2026-09-03")
                        .param("establecimiento", "P"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No escribiste ninguna linea para guardar")));
        org.junit.jupiter.api.Assertions.assertEquals(0, movements.count());
    }
}
