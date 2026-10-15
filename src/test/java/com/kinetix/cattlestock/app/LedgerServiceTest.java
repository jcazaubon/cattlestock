package com.kinetix.cattlestock.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.kinetix.cattlestock.data.MovementRepository;
import com.kinetix.cattlestock.domain.Column;
import com.kinetix.cattlestock.domain.DomainException;
import com.kinetix.cattlestock.domain.Ledger;
import com.kinetix.cattlestock.domain.Movement;
import com.kinetix.cattlestock.domain.Planilla;
import com.kinetix.cattlestock.domain.Species;

/** Pruebas de la capa de aplicacion contra H2 en memoria. Cada test configura su propio escenario. */
@SpringBootTest
@Transactional
class LedgerServiceTest {

    static final YearMonth SEP = YearMonth.of(2026, 9);
    static final YearMonth OCT = YearMonth.of(2026, 10);

    @Autowired
    LedgerService service;

    @Autowired
    MovementRepository movements;

    void configurar() {
        service.configureCompany("Empresa de prueba", SEP);
        service.createEstablishment("P", "Principal");
        service.createCategory("P", "toros", "Toros", Species.VACUNOS);
        service.createCategory("P", "toroseng", "Toros Engorde", Species.VACUNOS);
        service.createEstablishment("LM", "La Marta");
        service.createCategory("LM", "vacas", "Vacas", Species.VACUNOS);
        service.setOpening("P", "toros", 51);
        service.setOpening("P", "toroseng", 0);
        service.setOpening("LM", "vacas", 5);
    }

    static Planilla.Row fila(Planilla p, String categoryId) {
        return p.rows().stream().filter(r -> r.category().id().equals(categoryId)).findFirst().orElseThrow();
    }

    @Test
    void elAltaPersisteYApareceEnLaPlanilla() {
        configurar();
        service.addMovement(Movement.compra(UUID.randomUUID().toString(), LocalDate.of(2026, 9, 3), "P", "toros", 10));
        assertEquals(1, movements.count(), "debe quedar guardado");

        Planilla p = service.planilla("P", SEP);
        assertEquals(10, fila(p, "toros").columns().get(Column.COMPRA), "columna compra");
        assertEquals(61, fila(p, "toros").finalStock(), "final = anterior + compra");
    }

    @Test
    void unMovimientoRechazadoNoPersisteNada() {
        configurar();
        DomainException e = assertThrows(DomainException.class, () ->
                service.addMovement(Movement.venta(UUID.randomUUID().toString(), LocalDate.of(2026, 9, 3), "P", "toroseng", 1)));
        assertTrue(e.getMessage().contains("Stock insuficiente"), e.getMessage());
        assertEquals(0, movements.count(), "no debe quedar guardado");
    }

    @Test
    void unTrasladoPersistidoApareceEnAmbasPlanillas() {
        configurar();
        service.addMovement(Movement.traslado(UUID.randomUUID().toString(), LocalDate.of(2026, 9, 15),
                "P", "toros", "LM", "vacas", 10));

        Planilla principal = service.planilla("P", SEP);
        Planilla lm = service.planilla("LM", SEP);
        assertEquals(10, fila(principal, "toros").columns().get(Column.TRASLADO_SALE), "sale de Principal");
        assertEquals(41, fila(principal, "toros").finalStock(), "final Principal");
        assertEquals(10, fila(lm, "vacas").columns().get(Column.TRASLADO_ENTRA), "entra en La Marta");
        assertEquals(15, fila(lm, "vacas").finalStock(), "final La Marta");
    }

    @Test
    void unaCargaEnGrillaEsTodoONadaPersistido() {
        configurar();
        DomainException e = assertThrows(DomainException.class, () -> service.addBatch(List.of(
                Movement.compra(UUID.randomUUID().toString(), LocalDate.of(2026, 9, 3), "P", "toros", 10),
                Movement.venta(UUID.randomUUID().toString(), LocalDate.of(2026, 9, 3), "P", "toroseng", 1))));
        assertTrue(e.getMessage().contains("Stock insuficiente"), e.getMessage());
        assertEquals(0, movements.count(), "no debe quedar nada guardado");

        service.addBatch(List.of(
                Movement.compra(UUID.randomUUID().toString(), LocalDate.of(2026, 9, 3), "P", "toros", 10),
                Movement.nacimiento(UUID.randomUUID().toString(), LocalDate.of(2026, 9, 3), "P", "toros", 5)));
        assertEquals(2, movements.count(), "el lote valido se guarda completo");

        Planilla p = service.planilla("P", SEP);
        assertEquals(10, fila(p, "toros").columns().get(Column.COMPRA), "compra");
        assertEquals(5, fila(p, "toros").columns().get(Column.NACIMIENTO), "nacimiento");
        assertEquals(66, fila(p, "toros").finalStock(), "final");
    }

    @Test
    void laVistaGeneralDeExistenciaFiltraYCalculaALaFecha() {
        configurar();
        service.addMovement(Movement.compra(UUID.randomUUID().toString(), LocalDate.of(2026, 9, 3), "P", "toros", 10));

        List<Ledger.ExistenceRow> todas = service.existencia(LocalDate.of(2026, 9, 30), null, null, null);
        assertEquals(3, todas.size(), "LM y P con todas sus categorias");

        List<Ledger.ExistenceRow> vacunos = service.existencia(LocalDate.of(2026, 9, 30), "P", Species.VACUNOS, "toros");
        assertEquals(1, vacunos.size(), "filtro por establecimiento, especie y categoria");
        assertEquals(61, vacunos.get(0).quantity(), "existencia con la compra incluida");
    }

    @Test
    void elCierrePersisteYSigueBloqueandoTrasReconstruirDesdeLaBase() {
        configurar();
        service.closeMonth("P", SEP);
        assertTrue(service.planilla("P", SEP).closed(), "la planilla queda cerrada");

        DomainException e = assertThrows(DomainException.class, () ->
                service.addMovement(Movement.compra(UUID.randomUUID().toString(), LocalDate.of(2026, 9, 20), "P", "toros", 1)));
        assertTrue(e.getMessage().contains("cerrada"), e.getMessage());
        assertEquals(0, movements.count(), "no debe quedar guardado");

        service.reopenLastMonth("P");
        assertFalse(service.planilla("P", SEP).closed(), "reabierta");
        service.addMovement(Movement.compra(UUID.randomUUID().toString(), LocalDate.of(2026, 9, 20), "P", "toros", 1));
        assertEquals(1, movements.count(), "despues de reabrir se puede cargar");
    }

    @Test
    void elCierreFueraDeOrdenSeRechaza() {
        configurar();
        DomainException e = assertThrows(DomainException.class, () -> service.closeMonth("P", OCT));
        assertTrue(e.getMessage().contains("proximo mes a cerrar"), e.getMessage());
    }

    @Test
    void elStockInicialNegativoSeRechaza() {
        configurar();
        DomainException e = assertThrows(DomainException.class, () -> service.setOpening("P", "toros", -1));
        assertTrue(e.getMessage().contains("negativo"), e.getMessage());
    }

    @Test
    void unaCategoriaNoPerteneceAOtroEstablecimiento() {
        configurar();
        DomainException e = assertThrows(DomainException.class, () ->
                service.addMovement(Movement.compra(UUID.randomUUID().toString(), LocalDate.of(2026, 9, 2), "LM", "toros", 1)));
        assertTrue(e.getMessage().contains("no existe en"), e.getMessage());
        assertEquals(0, movements.count(), "no debe quedar guardado");
    }
}
