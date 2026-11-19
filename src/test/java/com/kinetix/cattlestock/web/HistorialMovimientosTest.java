package com.kinetix.cattlestock.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import com.kinetix.cattlestock.app.LedgerService.CambiosMovimiento;
import com.kinetix.cattlestock.app.UserService;
import com.kinetix.cattlestock.data.MovementEntity;
import com.kinetix.cattlestock.data.MovementRepository;
import com.kinetix.cattlestock.data.UserEntity;
import com.kinetix.cattlestock.domain.DomainException;
import com.kinetix.cattlestock.domain.Movement;
import com.kinetix.cattlestock.domain.Species;

/** El administrador ve, corrige y borra (en forma logica) movimientos; cada cambio revalida el historial. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HistorialMovimientosTest {

    static final YearMonth SEP = YearMonth.of(2026, 9);
    static final LocalDate FIN = LocalDate.of(2026, 9, 30);

    @Autowired MockMvc mockMvc;
    @Autowired LedgerService service;
    @Autowired UserService userService;
    @Autowired MovementRepository movements;

    Long norte;
    Long sur;

    /** Norte: dos establecimientos con toros (10 y 0) y vacas. Sur: uno aparte. */
    void configurar() {
        norte = service.createCompany("Ganadera Norte", SEP);
        service.createEstablishment(norte, "N1", "Campo Norte");
        service.createEstablishment(norte, "N2", "Campo Dos");
        service.createCategory(norte, "toros", "Toros", Species.VACUNOS);
        service.createCategory(norte, "vacas", "Vacas", Species.VACUNOS);
        service.setOpening("N1", "toros", 10);

        sur = service.createCompany("Ganadera Sur", SEP);
        service.createEstablishment(sur, "S1", "Campo Sur");
        service.createCategory(sur, "toros", "Toros", Species.VACUNOS);
        service.setOpening("S1", "toros", 77);

        userService.crear("adminnorte", "x", UserEntity.Role.ADMIN, norte, List.of());
        userService.crear("pepe", "x", UserEntity.Role.CARGA, norte, List.of("N1"));
    }

    org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
        return user("adminnorte").roles("ADMIN");
    }

    int stock(String est, String cat) {
        return service.existencia(service.companyOf(est), FIN, est, null, cat).get(0).quantity();
    }

    Long id(String businessId) {
        return movements.findAll().stream().filter(m -> m.getBusinessId().equals(businessId))
                .findFirst().orElseThrow().getId();
    }

    MovementEntity movimiento(String businessId) {
        return movements.findById(id(businessId)).orElseThrow();
    }

    CambiosMovimiento cambios(LocalDate fecha, String categoria, int cantidad) {
        return new CambiosMovimiento(fecha, categoria, cantidad, null, null, null);
    }

    // ---- corregir ----

    @Test
    void corregirCambiaLaCantidadLaFechaLaCategoriaYLaNotaYDejaQuienLoHizo() {
        configurar();
        service.addMovement(Movement.compra("c1", LocalDate.of(2026, 9, 3), "N1", "toros", 5));
        assertEquals(15, stock("N1", "toros"));

        service.actualizarMovimiento(norte, id("c1"), "adminnorte", new CambiosMovimiento(
                LocalDate.of(2026, 9, 8), "vacas", 7, null, null, "  era de vacas  "));
        MovementEntity m = movimiento("c1");
        assertEquals(LocalDate.of(2026, 9, 8), m.getDate());
        assertEquals("vacas", m.getCategoryCode());
        assertEquals(7, m.getQuantity());
        assertEquals("era de vacas", m.getNote());
        assertEquals("adminnorte", m.getModifiedBy());
        assertTrue(m.getModifiedAt() != null);
        assertEquals(10, stock("N1", "toros"), "ya no suma a toros");
        assertEquals(7, stock("N1", "vacas"));
    }

    @Test
    void corregirValidaTodoElHistorialYSiNoCierraLoRevierteYAvisa() {
        configurar();
        service.addMovement(Movement.compra("c1", LocalDate.of(2026, 9, 3), "N1", "toros", 5));
        service.addMovement(Movement.venta("v1", LocalDate.of(2026, 9, 10), "N1", "toros", 14)); // usa lo de la compra
        // bajar la compra a 1 dejaria el saldo del dia 10 en negativo (10 + 1 - 14)
        var e = assertThrows(DomainException.class, () ->
                service.actualizarMovimiento(norte, id("c1"), "adminnorte", cambios(LocalDate.of(2026, 9, 3), "toros", 1)));
        assertTrue(e.getMessage().startsWith("No se puede guardar el cambio"), e.getMessage());
        assertEquals(5, movimiento("c1").getQuantity(), "se revirtio");
        assertEquals(null, movimiento("c1").getModifiedBy(), "y no quedo marcado como editado");
        assertEquals(1, stock("N1", "toros"));
        // mover la compra a una fecha posterior a la venta tampoco cierra
        assertThrows(DomainException.class, () ->
                service.actualizarMovimiento(norte, id("c1"), "adminnorte", cambios(LocalDate.of(2026, 9, 20), "toros", 5)));
        assertEquals(LocalDate.of(2026, 9, 3), movimiento("c1").getDate());
        // pero un cambio valido, que en un paso intermedio habria sido invalido, se acepta: la compra baja de dia
        service.actualizarMovimiento(norte, id("c1"), "adminnorte", cambios(LocalDate.of(2026, 9, 1), "toros", 6));
        assertEquals(2, stock("N1", "toros"));
    }

    @Test
    void corregirRespetaLasReglasBasicasYSoloAfectaALaEmpresaDelAdministrador() {
        configurar();
        service.addMovement(Movement.compra("c1", LocalDate.of(2026, 9, 3), "N1", "toros", 5));
        assertThrows(DomainException.class, () -> service.actualizarMovimiento(norte, id("c1"), "a", cambios(LocalDate.of(2026, 9, 3), "toros", 0)));
        assertThrows(DomainException.class, () -> service.actualizarMovimiento(norte, id("c1"), "a", cambios(LocalDate.of(2026, 9, 3), "inventada", 2)));
        // un movimiento de otra empresa no existe para este administrador
        service.addMovement(Movement.compra("s1", LocalDate.of(2026, 9, 3), "S1", "toros", 2));
        var ajeno = assertThrows(DomainException.class, () ->
                service.actualizarMovimiento(norte, id("s1"), "a", cambios(LocalDate.of(2026, 9, 3), "toros", 99)));
        assertEquals("El movimiento no existe", ajeno.getMessage());
        assertEquals(2, movimiento("s1").getQuantity());
        // una categoria deshabilitada no se puede elegir al corregir
        service.setCategoryEnabled(norte, "vacas", false);
        var apagada = assertThrows(DomainException.class, () ->
                service.actualizarMovimiento(norte, id("c1"), "a", cambios(LocalDate.of(2026, 9, 3), "vacas", 5)));
        assertTrue(apagada.getMessage().contains("deshabilitada"), apagada.getMessage());
    }

    @Test
    void enUnTrasladoSeCorrigeLaCategoriaDeDestinoPeroNoElEstablecimiento() {
        configurar();
        service.addMovement(Movement.traslado("t1", LocalDate.of(2026, 9, 5), "N1", "toros", "N2", "toros", 4));
        assertEquals(4, stock("N2", "toros"));
        service.actualizarMovimiento(norte, id("t1"), "adminnorte", new CambiosMovimiento(
                LocalDate.of(2026, 9, 5), "toros", 3, "vacas", null, null));
        MovementEntity m = movimiento("t1");
        assertEquals("N2", m.getDestEstablishmentCode(), "el destino no cambia");
        assertEquals("vacas", m.getDestCategoryCode());
        assertEquals(3, stock("N2", "vacas"));
        assertEquals(0, stock("N2", "toros"));
    }

    @Test
    void enUnaMortandadSeCorrigenLosCueros() {
        configurar();
        service.addMovement(Movement.mortandad("m1", LocalDate.of(2026, 9, 5), "N1", "toros", 3, 1));
        service.actualizarMovimiento(norte, id("m1"), "adminnorte", new CambiosMovimiento(
                LocalDate.of(2026, 9, 5), "toros", 3, null, 2, null));
        assertEquals(2, movimiento("m1").getCueros());
        assertThrows(DomainException.class, () -> service.actualizarMovimiento(norte, id("m1"), "a",
                new CambiosMovimiento(LocalDate.of(2026, 9, 5), "toros", 3, null, 9, null)), "mas cueros que muertes");
    }

    // ---- eliminar y restaurar (borrado logico) ----

    @Test
    void eliminarEsLogicoElStockLoDejaDeContarYSePuedeRestaurar() {
        configurar();
        service.addMovement(Movement.compra("c1", LocalDate.of(2026, 9, 3), "N1", "toros", 5));
        assertEquals(15, stock("N1", "toros"));

        service.eliminarMovimiento(norte, id("c1"), "adminnorte");
        MovementEntity m = movimiento("c1");
        assertTrue(m.isDeleted(), "la fila sigue en la base");
        assertEquals("adminnorte", m.getDeletedBy());
        assertTrue(m.getDeletedAt() != null);
        assertEquals(10, stock("N1", "toros"), "ya no cuenta");
        assertFalse(service.planilla("N1", SEP).rows().isEmpty());
        assertEquals(0, service.planilla("N1", SEP).totalGeneral().columns().get(com.kinetix.cattlestock.domain.Column.COMPRA));
        // eliminar dos veces no falla ni cambia nada
        service.eliminarMovimiento(norte, id("c1"), "otro");
        assertEquals("adminnorte", movimiento("c1").getDeletedBy());
        // un movimiento eliminado no se corrige: primero se restaura
        assertThrows(DomainException.class, () ->
                service.actualizarMovimiento(norte, id("c1"), "a", cambios(LocalDate.of(2026, 9, 3), "toros", 9)));

        service.restaurarMovimiento(norte, id("c1"));
        assertFalse(movimiento("c1").isDeleted());
        assertEquals(null, movimiento("c1").getDeletedBy());
        assertEquals(15, stock("N1", "toros"));
    }

    @Test
    void noSePuedeEliminarUnMovimientoSiDejaElStockNegativoMasAdelante() {
        configurar();
        service.addMovement(Movement.compra("c1", LocalDate.of(2026, 9, 3), "N1", "toros", 20));
        service.addMovement(Movement.venta("v1", LocalDate.of(2026, 9, 10), "N1", "toros", 25)); // 10 + 20 - 25
        var e = assertThrows(DomainException.class, () -> service.eliminarMovimiento(norte, id("c1"), "adminnorte"));
        assertTrue(e.getMessage().startsWith("No se puede eliminar el movimiento"), e.getMessage());
        assertFalse(movimiento("c1").isDeleted(), "quedo como estaba");
        assertEquals(5, stock("N1", "toros"));
        // eliminar primero la venta si deja
        service.eliminarMovimiento(norte, id("v1"), "adminnorte");
        service.eliminarMovimiento(norte, id("c1"), "adminnorte");
        assertEquals(10, stock("N1", "toros"));
        // y restaurar en el orden contrario: la venta sola no cierra
        var e2 = assertThrows(DomainException.class, () -> service.restaurarMovimiento(norte, id("v1")));
        assertTrue(e2.getMessage().startsWith("No se puede restaurar el movimiento"), e2.getMessage());
        assertTrue(movimiento("v1").isDeleted());
        service.restaurarMovimiento(norte, id("c1"));
        service.restaurarMovimiento(norte, id("v1"));
        assertEquals(5, stock("N1", "toros"));
    }

    @Test
    void unMovimientoDeUnMesCerradoNoSeToca() {
        configurar();
        service.addMovement(Movement.compra("c1", LocalDate.of(2026, 9, 3), "N1", "toros", 5));
        service.closeMonth("N1", SEP);
        var e = assertThrows(DomainException.class, () -> service.eliminarMovimiento(norte, id("c1"), "adminnorte"));
        assertTrue(e.getMessage().contains("esta cerrada") && e.getMessage().contains("reabrila"), e.getMessage());
        assertThrows(DomainException.class, () ->
                service.actualizarMovimiento(norte, id("c1"), "a", cambios(LocalDate.of(2026, 9, 3), "toros", 6)));
        assertFalse(movimiento("c1").isDeleted());
        assertEquals(5, movimiento("c1").getQuantity());
        service.reopenLastMonth("N1");
        service.eliminarMovimiento(norte, id("c1"), "adminnorte");
        assertTrue(movimiento("c1").isDeleted());
    }

    @Test
    void unTrasladoEliminadoDejaDeContarEnLosDosLados() {
        configurar();
        service.addMovement(Movement.traslado("t1", LocalDate.of(2026, 9, 5), "N1", "toros", "N2", "toros", 4));
        assertEquals(6, stock("N1", "toros"));
        assertEquals(4, stock("N2", "toros"));
        service.eliminarMovimiento(norte, id("t1"), "adminnorte");
        assertEquals(10, stock("N1", "toros"));
        assertEquals(0, stock("N2", "toros"));
    }

    // ---- pantalla ----

    @Test
    void elHistorialListaLosMovimientosDelMesConSusBotonesYFiltra() throws Exception {
        configurar();
        service.addMovement(Movement.compra("c1", LocalDate.of(2026, 9, 3), "N1", "toros", 5));
        service.addMovement(Movement.venta("v1", LocalDate.of(2026, 9, 10), "N1", "toros", 2));
        service.addMovement(Movement.nacimiento("n1", LocalDate.of(2026, 10, 20), "N1", "vacas", 9)); // otro mes
        service.addMovement(Movement.compra("s1", LocalDate.of(2026, 9, 3), "S1", "toros", 3));        // otra empresa
        mockMvc.perform(get("/historial").param("mes", "2026-09").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Historial de movimientos")))
                .andExpect(content().string(containsString("2 movimientos")))
                .andExpect(content().string(containsString("03/09/2026")))
                .andExpect(content().string(containsString("<td>Compra</td>")))
                .andExpect(content().string(containsString("<td>Venta</td>")))
                .andExpect(content().string(containsString("action=\"/historial/" + id("c1") + "/eliminar\"")))
                .andExpect(content().string(containsString(">Editar</button>")))
                .andExpect(content().string(not(containsString("<td>Nacimiento</td>"))))
                .andExpect(content().string(not(containsString("Campo Sur"))));
        mockMvc.perform(get("/historial").param("mes", "2026-09").param("tipo", "VENTA").with(admin()))
                .andExpect(content().string(containsString("1 movimiento")))
                .andExpect(content().string(not(containsString("<td>Compra</td>"))));
        mockMvc.perform(get("/historial").param("mes", "2026-10").with(admin()))
                .andExpect(content().string(containsString("<td>Nacimiento</td>")));
        mockMvc.perform(get("/historial").param("mes", "2026-01").with(admin()))
                .andExpect(content().string(containsString("No hay movimientos con esos filtros.")));
    }

    @Test
    void laFilaEnEdicionTraeSusCamposYLosEliminadosSoloSeMuestranSiSePide() throws Exception {
        configurar();
        service.addMovement(Movement.compra("c1", LocalDate.of(2026, 9, 3), "N1", "toros", 5));
        service.addMovement(Movement.venta("v1", LocalDate.of(2026, 9, 10), "N1", "toros", 2));
        service.eliminarMovimiento(norte, id("v1"), "adminnorte");
        mockMvc.perform(get("/historial").param("mes", "2026-09").param("editar", String.valueOf(id("c1"))).with(admin()))
                .andExpect(content().string(containsString("form=\"editar\"")))
                .andExpect(content().string(containsString("name=\"cantidad\"")))
                .andExpect(content().string(containsString(">Guardar</button>")))
                .andExpect(content().string(containsString(">Cancelar</button>")))
                .andExpect(content().string(not(containsString("<td>Venta</td>"))));
        mockMvc.perform(get("/historial").param("mes", "2026-09").param("eliminados", "1").with(admin()))
                .andExpect(content().string(containsString("<td>Venta</td>")))
                .andExpect(content().string(containsString("Eliminado por adminnorte")))
                .andExpect(content().string(containsString(">Restaurar</button>")))
                .andExpect(content().string(containsString("tr class=\"eliminado\"")));
    }

    @Test
    void lasAccionesDePantallaCorrigenEliminanRestauranYAvisanLosMotivos() throws Exception {
        configurar();
        service.addMovement(Movement.compra("c1", LocalDate.of(2026, 9, 3), "N1", "toros", 5));
        service.addMovement(Movement.venta("v1", LocalDate.of(2026, 9, 10), "N1", "toros", 14));
        // corregir bien
        mockMvc.perform(post("/historial/" + id("c1") + "/editar").with(admin()).with(csrf())
                        .param("fecha", "2026-09-04").param("categoria", "toros").param("cantidad", "6")
                        .param("nota", "ajustado").param("mes", "2026-09").param("establecimiento", "N1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/historial?establecimiento=N1&mes=2026-09&aviso=Movimiento+actualizado."));
        assertEquals(6, movimiento("c1").getQuantity());
        assertEquals("ajustado", movimiento("c1").getNote());
        // corregir mal: vuelve a la fila en edicion con el motivo
        mockMvc.perform(post("/historial/" + id("c1") + "/editar").with(admin()).with(csrf())
                        .param("fecha", "2026-09-04").param("categoria", "toros").param("cantidad", "1")
                        .param("mes", "2026-09"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("editar=" + id("c1"))))
                .andExpect(header().string("Location", containsString("error=No+se+puede+guardar+el+cambio")));
        mockMvc.perform(post("/historial/" + id("c1") + "/editar").with(admin()).with(csrf())
                        .param("fecha", "no-es-fecha").param("categoria", "toros").param("cantidad", "1"))
                .andExpect(header().string("Location", containsString("error=La+fecha+no+es+v%C3%A1lida")));
        mockMvc.perform(post("/historial/" + id("c1") + "/editar").with(admin()).with(csrf())
                        .param("fecha", "2026-09-04").param("categoria", "toros").param("cantidad", "mucho"))
                .andExpect(header().string("Location", containsString("error=La+cantidad+debe+ser+un+numero+entero")));
        // eliminar y restaurar
        mockMvc.perform(post("/historial/" + id("v1") + "/eliminar").with(admin()).with(csrf()).param("mes", "2026-09"))
                .andExpect(redirectedUrl("/historial?mes=2026-09&aviso=Movimiento+eliminado.+Queda+en+el+historial+y+se+puede+restaurar."));
        assertTrue(movimiento("v1").isDeleted());
        mockMvc.perform(post("/historial/" + id("v1") + "/restaurar").with(admin()).with(csrf()).param("mes", "2026-09"))
                .andExpect(redirectedUrl("/historial?mes=2026-09&aviso=Movimiento+restaurado."));
        assertFalse(movimiento("v1").isDeleted());
        // la compra no se puede eliminar porque la venta la necesita
        mockMvc.perform(post("/historial/" + id("c1") + "/eliminar").with(admin()).with(csrf()))
                .andExpect(header().string("Location", containsString("error=No+se+puede+eliminar+el+movimiento")));
    }

    @Test
    void soloLosAdministradoresEntranYNingunoTocaMovimientosDeOtraEmpresaNiDeUnEstablecimientoQueNoOpera() throws Exception {
        configurar();
        service.addMovement(Movement.compra("c1", LocalDate.of(2026, 9, 3), "N1", "toros", 5));
        service.addMovement(Movement.compra("s1", LocalDate.of(2026, 9, 3), "S1", "toros", 3));
        // quien solo carga no ve ni toca el historial
        mockMvc.perform(get("/historial").with(user("pepe").roles("CARGA"))).andExpect(status().isForbidden());
        mockMvc.perform(post("/historial/" + id("c1") + "/eliminar").with(user("pepe").roles("CARGA")).with(csrf()))
                .andExpect(status().isForbidden());
        assertFalse(movimiento("c1").isDeleted());
        // el administrador de Norte no puede eliminar ni corregir uno de Sur
        mockMvc.perform(post("/historial/" + id("s1") + "/eliminar").with(admin()).with(csrf()))
                .andExpect(header().string("Location", containsString("error=El+movimiento+no+existe")));
        assertFalse(movimiento("s1").isDeleted());
        // un administrador con solo N2 tildado no toca los movimientos de N1
        userService.crear("parcial", "x", UserEntity.Role.ADMIN, norte, List.of("N2"));
        mockMvc.perform(post("/historial/" + id("c1") + "/eliminar").with(user("parcial").roles("ADMIN")).with(csrf()))
                .andExpect(header().string("Location", containsString("error=No+ten%C3%A9s+acceso")));
        assertFalse(movimiento("c1").isDeleted());
        mockMvc.perform(get("/historial").param("mes", "2026-09").with(user("parcial").roles("ADMIN")))
                .andExpect(content().string(containsString("No hay movimientos con esos filtros.")));
    }

    @Test
    void laBarraOfreceElHistorialSoloAlosAdministradores() throws Exception {
        configurar();
        mockMvc.perform(get("/").with(admin()))
                .andExpect(content().string(containsString("href=\"/historial\"")));
        mockMvc.perform(get("/").with(user("pepe").roles("CARGA")))
                .andExpect(content().string(not(containsString("href=\"/historial\""))));
    }

    @Test
    void elAltaPermiteSumarMasDeUnMovimientoAlLoteYLosGuardaJuntos() throws Exception {
        configurar();
        mockMvc.perform(get("/alta").param("establecimiento", "N1").with(admin()))
                .andExpect(content().string(containsString("Agregar otro movimiento")))
                .andExpect(content().string(containsString("entran todos o no entra ninguno")));
        mockMvc.perform(get("/alta/linea").param("establecimiento", "N1").param("categoria0", "toros").param("tipo0", "COMPRA")
                        .param("cantidad0", "3").with(admin()))
                .andExpect(content().string(containsString("name=\"categoria1\"")));
        mockMvc.perform(post("/movimientos").with(admin()).with(csrf())
                        .param("fecha", "2026-09-04").param("establecimiento", "N1")
                        .param("categoria0", "toros").param("tipo0", "COMPRA").param("cantidad0", "3")
                        .param("categoria1", "vacas").param("tipo1", "NACIMIENTO").param("cantidad1", "2")
                        .param("categoria2", "toros").param("tipo2", "VENTA").param("cantidad2", "1"))
                .andExpect(status().is3xxRedirection());
        assertEquals(3, movements.count());
        assertEquals(12, stock("N1", "toros"));
        assertEquals(2, stock("N1", "vacas"));
        // si una linea falla, no entra ninguna
        mockMvc.perform(post("/movimientos").with(admin()).with(csrf())
                        .param("fecha", "2026-09-05").param("establecimiento", "N1")
                        .param("categoria0", "toros").param("tipo0", "COMPRA").param("cantidad0", "3")
                        .param("categoria1", "vacas").param("tipo1", "VENTA").param("cantidad1", "99"))
                .andExpect(status().isOk());
        assertEquals(3, movements.count());
    }
}
