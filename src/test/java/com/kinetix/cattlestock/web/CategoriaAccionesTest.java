package com.kinetix.cattlestock.web;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import com.kinetix.cattlestock.data.CategoryEntity;
import com.kinetix.cattlestock.data.UserEntity;
import com.kinetix.cattlestock.domain.DomainException;
import com.kinetix.cattlestock.domain.Movement;
import com.kinetix.cattlestock.domain.Planilla;
import com.kinetix.cattlestock.domain.Species;

/** Acciones sobre las categorias de la empresa: cambiar el nombre, habilitar o deshabilitar y dar de baja. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CategoriaAccionesTest {

    static final YearMonth SEP = YearMonth.of(2026, 9);
    static final LocalDate FIN = LocalDate.of(2026, 9, 30);

    @Autowired MockMvc mockMvc;
    @Autowired LedgerService service;
    @Autowired UserService userService;

    Long norte;
    Long sur;

    /** Norte: toros (51 de stock), vacas y novillos sin uso. Sur: una categoria propia. */
    void configurar() {
        norte = service.createCompany("Ganadera Norte", SEP);
        service.createEstablishment(norte, "N1", "Campo Norte");
        service.createCategory(norte, "toros", "Toros", Species.VACUNOS);
        service.createCategory(norte, "vacas", "Vacas", Species.VACUNOS);
        service.createCategory(norte, "novillos", "Novillos", Species.VACUNOS);
        service.setOpening("N1", "toros", 51);
        service.setOpening("N1", "vacas", 0);     // un 0 incluye la categoria en el establecimiento
        service.setOpening("N1", "novillos", 0);

        sur = service.createCompany("Ganadera Sur", SEP);
        service.createEstablishment(sur, "S1", "Campo Sur");
        service.createCategory(sur, "sureno", "Sureno", Species.VACUNOS);

        userService.crear("adminnorte", "x", UserEntity.Role.ADMIN, norte, List.of());
        userService.crear("pepe", "x", UserEntity.Role.CARGA, norte, List.of("N1"));
    }

    org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
        return user("adminnorte").roles("ADMIN");
    }

    List<String> nombres() {
        return service.categoriasDeEmpresa(norte).stream().map(CategoryEntity::getName).toList();
    }

    // ---- modificacion: solo el nombre ----

    @Test
    void renombrarCambiaElNombreYNoElCodigoNiLoQueCuelgaDeEl() {
        configurar();
        service.renameCategory(norte, "toros", "  Toros Reproductores ");
        assertEquals(List.of("Toros Reproductores", "Vacas", "Novillos"), nombres());
        assertEquals("toros", service.categoriasDeEmpresa(norte).get(0).getCode());
        assertEquals(51, service.existencia(norte, FIN, "N1", null, "toros").get(0).quantity(), "el stock sigue");
        // conservarle el mismo nombre no es un duplicado
        service.renameCategory(norte, "toros", "toros reproductores");
        assertEquals("toros reproductores", service.categoriasDeEmpresa(norte).get(0).getName());
    }

    @Test
    void renombrarNoAceptaNombresVaciosNiRepetidosNiCategoriasDeOtraEmpresa() {
        configurar();
        assertThrows(DomainException.class, () -> service.renameCategory(norte, "toros", "  "));
        var repetido = assertThrows(DomainException.class, () -> service.renameCategory(norte, "toros", "VACAS"));
        assertTrue(repetido.getMessage().contains("Ya hay una categoria llamada"), repetido.getMessage());
        var ajena = assertThrows(DomainException.class, () -> service.renameCategory(norte, "sureno", "Hackeada"));
        assertTrue(ajena.getMessage().contains("no existe"), ajena.getMessage());
        assertEquals("Sureno", service.categoriasDeEmpresa(sur).get(0).getName());
        // el mismo nombre en otra empresa no cuenta como repetido
        service.renameCategory(sur, "sureno", "Toros");
    }

    // ---- habilitar / deshabilitar ----

    @Test
    void unaCategoriaDeshabilitadaNoSeOfreceNiAceptaMovimientosPeroSeVuelveAhabilitar() {
        configurar();
        service.setCategoryEnabled(norte, "novillos", false);
        assertEquals(List.of("Toros", "Vacas"), service.categoriasHabilitadasDe("N1").stream().map(CategoryEntity::getName).toList());
        assertEquals(3, service.categoriasDeEmpresa(norte).size(), "sigue en el catalogo");

        var e = assertThrows(DomainException.class, () -> service.addMovement(
                Movement.nacimiento("m1", LocalDate.of(2026, 9, 5), "N1", "novillos", 3)));
        assertTrue(e.getMessage().contains("deshabilitada") && e.getMessage().contains("Novillos"), e.getMessage());
        // tampoco como destino de un cambio de categoria, y un lote entero se rechaza
        assertThrows(DomainException.class, () -> service.addMovement(
                Movement.cambioCategoria("m2", LocalDate.of(2026, 9, 5), "N1", "toros", "novillos", 1)));
        assertThrows(DomainException.class, () -> service.addBatch(List.of(
                Movement.nacimiento("m3", LocalDate.of(2026, 9, 5), "N1", "toros", 1),
                Movement.nacimiento("m4", LocalDate.of(2026, 9, 5), "N1", "novillos", 1))));
        assertEquals(51, service.existencia(norte, FIN, "N1", null, "toros").get(0).quantity(), "nada quedo a medias");

        service.setCategoryEnabled(norte, "novillos", true);
        service.addMovement(Movement.nacimiento("m5", LocalDate.of(2026, 9, 5), "N1", "novillos", 3));
        assertEquals(3, service.existencia(norte, FIN, "N1", null, "novillos").get(0).quantity());
    }

    @Test
    void laPlanillaYLaExistenciaOcultanLasDeshabilitadasEnCeroPeroNoLasQueTienenAnimales() {
        configurar();
        service.setCategoryEnabled(norte, "novillos", false); // sin uso
        service.setCategoryEnabled(norte, "toros", false);    // con 51 animales
        Planilla p = service.planilla("N1", SEP);
        var filas = p.rows().stream().map(r -> r.category().id()).toList();
        assertFalse(filas.contains("novillos"), "deshabilitada y en cero: sale de la planilla");
        assertTrue(filas.contains("toros"), "deshabilitada pero con stock: se queda, para no esconder animales");
        assertTrue(filas.contains("vacas"), "habilitada en cero: se queda");

        var ex = service.existencia(norte, FIN, null, null, null).stream().map(r -> r.category().id()).toList();
        assertFalse(ex.contains("novillos"));
        assertTrue(ex.contains("toros"));
        var det = service.existenciaDetallada(norte, FIN, null, null, null).stream().map(d -> d.category().id()).toList();
        assertFalse(det.contains("novillos"));
        assertTrue(det.contains("toros"));
        // cuando se la vuelve a habilitar, reaparece
        service.setCategoryEnabled(norte, "novillos", true);
        assertTrue(service.planilla("N1", SEP).rows().stream().anyMatch(r -> r.category().id().equals("novillos")));
    }

    // ---- baja ----

    @Test
    void laBajaSoloSepermiteParaUnaCategoriaQueNuncaSeUso() {
        configurar();
        // con stock inicial: no
        var conStock = assertThrows(DomainException.class, () -> service.deleteCategory(norte, "toros"));
        assertTrue(conStock.getMessage().contains("Podes deshabilitarla"), conStock.getMessage());
        // con un movimiento (aunque su stock inicial sea 0): tampoco
        service.setOpening("N1", "vacas", 0);
        service.addMovement(Movement.nacimiento("m1", LocalDate.of(2026, 9, 5), "N1", "vacas", 2));
        assertThrows(DomainException.class, () -> service.deleteCategory(norte, "vacas"));
        // como destino de un cambio de categoria, cuenta como uso
        service.addMovement(Movement.cambioCategoria("m2", LocalDate.of(2026, 9, 6), "N1", "toros", "novillos", 1));
        assertThrows(DomainException.class, () -> service.deleteCategory(norte, "novillos"));
        assertEquals(3, service.categoriasDeEmpresa(norte).size(), "no se borro nada");

        // una sin uso (en esta empresa no hay otra): se crea una nueva y se da de baja
        service.createCategory(norte, "terneros", "Terneros", Species.VACUNOS);
        service.setOpening("N1", "terneros", 0); // un stock inicial en cero no cuenta como uso y se limpia con la baja
        service.deleteCategory(norte, "terneros");
        assertEquals(List.of("Toros", "Vacas", "Novillos"), nombres());
        assertFalse(service.planilla("N1", SEP).rows().stream().anyMatch(r -> r.category().id().equals("terneros")));
        assertFalse(service.aperturasDeEmpresa(norte).containsKey("N1|terneros"), "tampoco queda su stock en cero");
        // y su codigo queda libre
        service.createCategory(norte, "terneros", "Terneros", Species.VACUNOS);
    }

    // ---- pantalla ----

    @Test
    void laListaTraeBotonesDeAccionYElNombreSeEditaEnLaMismaFila() throws Exception {
        configurar();
        service.setCategoryEnabled(norte, "novillos", false);
        mockMvc.perform(get("/configuracion").param("seccion", "categorias").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<th>Estado</th>")))
                .andExpect(content().string(containsString("Habilitada")))
                .andExpect(content().string(containsString("Deshabilitada")))
                .andExpect(content().string(containsString("action=\"/configuracion/categorias/toros/estado\"")))
                .andExpect(content().string(containsString("aria-label=\"Deshabilitar Toros\"")))
                .andExpect(content().string(containsString("aria-label=\"Habilitar Novillos\"")))
                .andExpect(content().string(containsString("action=\"/configuracion/categorias/toros/baja\"")))
                .andExpect(content().string(containsString("name=\"editar\" value=\"toros\"")))
                .andExpect(content().string(containsString("<tr class=\"deshabilitada\">")));
        // en modo edicion: la fila trae el campo del nombre y Guardar/Cancelar, sin salir de la lista
        mockMvc.perform(get("/configuracion").param("seccion", "categorias").param("editar", "vacas").with(admin()))
                .andExpect(content().string(containsString("action=\"/configuracion/categorias/vacas/nombre\"")))
                .andExpect(content().string(containsString("name=\"nombre\" value=\"Vacas\"")))
                .andExpect(content().string(containsString("form=\"renombrar\"")))
                .andExpect(content().string(containsString("aria-label=\"Guardar nombre\"")))
                .andExpect(content().string(containsString("aria-label=\"Cancelar\"")));
    }

    @Test
    void lasAccionesDePantallaFuncionanYAvisanCuandoNoCorresponden() throws Exception {
        configurar();
        mockMvc.perform(post("/configuracion/categorias/vacas/nombre").with(admin()).with(csrf())
                        .param("nombre", "Vacas Madres"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/configuracion?seccion=categorias"));
        assertEquals("Vacas Madres", service.categoriasDeEmpresa(norte).get(1).getName());
        // un nombre repetido vuelve a la fila en edicion con el aviso
        mockMvc.perform(post("/configuracion/categorias/vacas/nombre").with(admin()).with(csrf())
                        .param("nombre", "Toros"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/configuracion?seccion=categorias&editar=vacas&error=Ya+hay+una+categoria+llamada+Toros"));

        mockMvc.perform(post("/configuracion/categorias/novillos/estado").with(admin()).with(csrf())
                        .param("habilitada", "false"))
                .andExpect(redirectedUrl("/configuracion?seccion=categorias"));
        assertFalse(service.categoriasDeEmpresa(norte).get(2).isEnabled());

        // baja: la que tiene stock no se puede; una sin uso, si
        mockMvc.perform(post("/configuracion/categorias/toros/baja").with(admin()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Location",
                        containsString("seccion=categorias&error=No+se+puede+dar+de+baja")));
        mockMvc.perform(post("/configuracion/categorias/novillos/baja").with(admin()).with(csrf()))
                .andExpect(redirectedUrl("/configuracion?seccion=categorias&aviso=Categoria+dada+de+baja."));
        assertEquals(2, service.categoriasDeEmpresa(norte).size());
    }

    @Test
    void cadaEmpresaSoloTocaSusCategoriasYSoloLoHaceElAdministrador() throws Exception {
        configurar();
        // el admin de Norte no puede cambiar, deshabilitar ni dar de baja la categoria de Sur
        for (String accion : List.of("nombre", "estado", "baja")) {
            mockMvc.perform(post("/configuracion/categorias/sureno/" + accion).with(admin()).with(csrf())
                            .param("nombre", "Hackeada").param("habilitada", "false"))
                    .andExpect(status().is3xxRedirection());
        }
        var sureno = service.categoriasDeEmpresa(sur).get(0);
        assertEquals("Sureno", sureno.getName());
        assertTrue(sureno.isEnabled());
        assertEquals(1, service.categoriasDeEmpresa(sur).size());
        // quien solo carga no tiene acceso a estas acciones
        mockMvc.perform(post("/configuracion/categorias/vacas/baja").with(user("pepe").roles("CARGA")).with(csrf()))
                .andExpect(status().isForbidden());
        assertEquals(3, service.categoriasDeEmpresa(norte).size());
    }

    @Test
    void laCargaRapidaYElAltaNoOfrecenLasDeshabilitadas() throws Exception {
        configurar();
        service.setCategoryEnabled(norte, "novillos", false);
        mockMvc.perform(get("/grilla/filas").param("establecimiento", "N1").with(admin()))
                .andExpect(content().string(containsString("name=\"compra_vacas\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("name=\"compra_novillos\""))));
        mockMvc.perform(get("/alta/lineas").param("establecimiento", "N1").with(admin()))
                .andExpect(content().string(containsString(">Vacas<")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString(">Novillos<"))));
        // y aunque alguien arme la peticion a mano, el servidor la rechaza
        mockMvc.perform(post("/grilla").with(admin()).with(csrf())
                        .param("establecimiento", "N1").param("fecha", "2026-09-03").param("compra_novillos", "4"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No escribiste ninguna cantidad")));
    }
}
