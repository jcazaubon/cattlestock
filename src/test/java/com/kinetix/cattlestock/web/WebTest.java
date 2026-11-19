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
@WithMockUser(username = "admin", roles = "SUPERADMIN")
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
        Long empresa = service.createCompany("Empresa de prueba", YearMonth.of(2026, 9));
        service.createEstablishment(empresa, "P", "Principal");
        service.createCategory(service.companyOf("P"), "toros", "Toros", Species.VACUNOS);
        service.createCategory(service.companyOf("P"), "toroseng", "Toros Engorde", Species.VACUNOS);
        service.setOpening("P", "toros", 51);
        service.setOpening("P", "toroseng", 0);
        service.createEstablishment(empresa, "LM", "La Marta");
        service.createCategory(service.companyOf("LM"), "vacas", "Vacas", Species.VACUNOS);
        service.setOpening("LM", "vacas", 5);
        // un 0 incluye la categoria en el establecimiento (un stock inicial vacio la deja afuera)
        service.setOpening("P", "vacas", 0);
        service.setOpening("LM", "toros", 0);
        service.setOpening("LM", "toroseng", 0);
        userService.crearSuperadmin("admin", "admin");
    }

    @Test
    void laPantallaDeAltaRenderizaConUnaLineaYBotonDeAgregar() throws Exception {
        configurar();
        mockMvc.perform(get("/alta"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cargar movimientos")))
                .andExpect(content().string(containsString("Principal")))
                .andExpect(content().string(containsString("Agregar otro movimiento")))
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
    void laGrillaConvierteLoContadoEnDiferenciaDeRecuento() throws Exception {
        configurar();
        // Toros arranca con 51: tras 10 a pastoreo se esperan 41, y contar 54 deja 13 a favor.
        mockMvc.perform(post("/grilla").with(csrf())
                        .param("establecimiento", "P")
                        .param("fecha", "2026-09-03")
                        .param("recuento_toros", "54")
                        .param("pastoreo_toros", "10"))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals(2, movements.count());
        org.junit.jupiter.api.Assertions.assertTrue(movements.findAll().stream().anyMatch(m ->
                m.getType() == com.kinetix.cattlestock.domain.MovementType.RECUENTO_MAS && m.getQuantity() == 13));
        org.junit.jupiter.api.Assertions.assertTrue(movements.findAll().stream().anyMatch(m ->
                m.getType() == com.kinetix.cattlestock.domain.MovementType.PASTOREO));
    }

    @Test
    void laGrillaFiltraPorEspecieOcultandoLasFilasSinPerderLoEscrito() throws Exception {
        configurar();
        service.createCategory(service.companyOf("P"), "yeguarizos", "Yeguarizos", Species.YEGUARIZOS);
        service.setOpening("P", "yeguarizos", 0);
        // el filtro esta en la pantalla
        mockMvc.perform(get("/grilla").param("establecimiento", "P").param("especie", "YEGUARIZOS"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<select name=\"especie\"")))
                .andExpect(content().string(containsString("<option value=\"YEGUARIZOS\" selected")))
                .andExpect(content().string(containsString("quedan ocultas")));
        // al filtrar, las filas de otra especie se ocultan pero siguen en el formulario con su valor
        mockMvc.perform(get("/grilla/filas").param("establecimiento", "P").param("especie", "YEGUARIZOS")
                        .param("compra_toros", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<tr hidden>")))
                .andExpect(content().string(containsString("name=\"compra_toros\"")))
                .andExpect(content().string(containsString("value=\"5\"")))
                .andExpect(content().string(containsString("name=\"compra_yeguarizos\"")));
        // sin filtro no se oculta nada; una especie rara se ignora; una sin categorias avisa
        mockMvc.perform(get("/grilla/filas").param("establecimiento", "P"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("<tr hidden>"))));
        mockMvc.perform(get("/grilla/filas").param("establecimiento", "P").param("especie", "INVENTADA"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("<tr hidden>"))));
        mockMvc.perform(get("/grilla/filas").param("establecimiento", "P").param("especie", "LANARES"))
                .andExpect(content().string(containsString("no tiene categorías de esa especie")));
        // lo escrito en una fila oculta se guarda igual
        mockMvc.perform(post("/grilla").with(csrf())
                        .param("establecimiento", "P").param("fecha", "2026-09-03")
                        .param("especie", "YEGUARIZOS")
                        .param("compra_toros", "5").param("compra_yeguarizos", "2"))
                .andExpect(status().is3xxRedirection());
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
                .andExpect(content().string(containsString("Cargar movimientos")));
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
    void elComboDeCategoriasAgrupaPorEspecieYEsElMismoParaTodosLosEstablecimientos() throws Exception {
        configurar();
        service.createCategory(service.companyOf("P"), "yeguarizos", "Yeguarizos", Species.YEGUARIZOS);
        mockMvc.perform(get("/existencia"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<select name=\"categoria\" id=\"categoria\">")))
                .andExpect(content().string(containsString("<optgroup label=\"Vacunos\">")))
                .andExpect(content().string(containsString("<optgroup label=\"Yeguarizos\">")));
        // el catalogo es de la empresa: elegir un establecimiento no lo cambia; elegir una especie, si.
        mockMvc.perform(get("/existencia/categorias").param("establecimiento", "LM"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Vacas<")))
                .andExpect(content().string(containsString(">Toros<")));
        mockMvc.perform(get("/existencia/categorias").param("especie", "YEGUARIZOS"))
                .andExpect(content().string(containsString(">Yeguarizos<")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Vacunos"))));
    }

    @Test
    void laExistenciaMuestraElMovimientoDelMesSoloCuandoLoHay() throws Exception {
        configurar();
        service.addMovement(com.kinetix.cattlestock.domain.Movement.compra(
                "e1", java.time.LocalDate.of(2026, 9, 3), "P", "toros", 10));
        mockMvc.perform(get("/existencia").param("establecimiento", "P").param("fecha", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Al 31/08")))
                .andExpect(content().string(containsString("Entradas")))
                .andExpect(content().string(containsString("animales al 30/09/2026")))
                .andExpect(content().string(containsString(">61<")));
        // sin movimientos en el mes, las columnas de movimiento sobran y no se muestran
        mockMvc.perform(get("/existencia").param("establecimiento", "LM").param("fecha", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Entradas"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Por establecimiento"))));
        // con "Todos" hay reparto para comparar, pero una sola tabla: no se separa por establecimiento
        mockMvc.perform(get("/existencia").param("fecha", "2026-09-30"))
                .andExpect(content().string(containsString("Por establecimiento")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("bloque-titulo"))));
        // categorias sin animales se pueden ocultar (Toros Engorde arranca en 0)
        mockMvc.perform(get("/existencia").param("establecimiento", "P").param("fecha", "2026-09-30")
                        .param("conExistencia", "1"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Toros Engorde</td>"))));
    }

    @Test
    void laExistenciaGlobalSumaLasCategoriasIgualesYDetallaLosMovimientos() throws Exception {
        configurar();
        // "Toros" es de la empresa y esta en los dos establecimientos: con "Todos" se suma en una sola fila
        service.setOpening("LM", "toros", 4);
        java.time.LocalDate d = java.time.LocalDate.of(2026, 9, 10);
        service.addMovement(com.kinetix.cattlestock.domain.Movement.mortandad("g1", d, "P", "toros", 2, 1));
        service.addMovement(com.kinetix.cattlestock.domain.Movement.consumo("g2", d, "LM", "toros", 1));
        service.addMovement(com.kinetix.cattlestock.domain.Movement.pastoreo("g3", d, "P", "toros", 3));
        service.addMovement(com.kinetix.cattlestock.domain.Movement.traslado("g4", d, "P", "toros", "LM", "vacas", 5));
        mockMvc.perform(get("/existencia").param("fecha", "2026-09-30"))
                .andExpect(status().isOk())
                // 51 + 4 - 2 mortandad - 1 consumo - 3 pastoreo - 5 traslado = 44 en Toros; las vacas de La Marta suben 5
                .andExpect(content().string(containsString("<td class=\"col-final\">44</td>")))
                .andExpect(content().string(containsString("Mortandad")))
                .andExpect(content().string(containsString("Consumo")))
                .andExpect(content().string(containsString("Pastoreo")))
                .andExpect(content().string(containsString("Traslado")))
                .andExpect(content().string(containsString("en el total se compensan")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("bloque-titulo"))))
                // no se piden movimientos que no ocurrieron
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Cambio de categoria"))));
    }

    @Test
    void elAltaMuestraTodosLosAnimalesDisponiblesYSeAcotaAlEstablecimiento() throws Exception {
        configurar();
        // sin establecimiento: todos los de la empresa, cada categoria con su cantidad
        mockMvc.perform(get("/alta"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Disponible en todos los establecimientos")))
                .andExpect(content().string(containsString("56 animales")))
                .andExpect(content().string(containsString("<li class=\"\"><span>Toros</span><strong>51</strong></li>")))
                .andExpect(content().string(containsString("<span>Vacas</span><strong>5</strong>")));
        // el fragmento que se refresca al elegir establecimiento trae solo los de ese
        mockMvc.perform(get("/alta/disponible").param("establecimiento", "LM").param("fecha", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Disponible en La Marta")))
                .andExpect(content().string(containsString("5 animales al 30/09/2026")))
                // las categorias son las de la empresa: Toros figura tambien en La Marta, con 0
                .andExpect(content().string(containsString("<li class=\"cero\"><span>Toros</span><strong>0</strong></li>")))
                .andExpect(content().string(containsString("<span>Vacas</span><strong>5</strong>")));
    }

    @Test
    void laConfiguracionSeCompletaPorPantalla() throws Exception {
        userService.crearSuperadmin("admin", "admin");
        service.createCompany("Ganadera", YearMonth.of(2026, 9));
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
                        .param("password", "secreto-123")
                        .param("rol", "CARGA")
                        .param("establecimientos", "P"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/configuracion").param("seccion", "usuarios"))
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
                .andExpect(content().string(containsString("Elegí la fecha")))
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
