package com.kinetix.cattlestock.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del nucleo del dominio. El escenario sale de la planilla de septiembre
 * 2026 con "Transferencias DE y A".
 */
class LedgerTest {

    static final YearMonth SEP = YearMonth.of(2026, 9);
    static final YearMonth OCT = YearMonth.of(2026, 10);

    interface Body { void run() throws Exception; }

    static void eq(Object expected, Object actual, String what) {
        if (!expected.equals(actual)) {
            throw new AssertionError(what + ": esperado " + expected + " pero fue " + actual);
        }
    }

    static void rejects(String contains, Body body) {
        DomainException e = assertThrows(DomainException.class, () -> body.run());
        if (!e.getMessage().contains(contains)) {
            throw new AssertionError("el error no menciona '" + contains + "': " + e.getMessage());
        }
    }

    static LocalDate sep(int day) { return LocalDate.of(2026, 9, day); }

    /** Principal con las categorias de la planilla, mas La Marta. Stock inicial al 1/9/2026. */
    static Ledger escenario() {
        Ledger l = new Ledger("Empresa de prueba", SEP);
        l.addEstablishment("P", "Principal");
        l.addCategory("P", "toros", "Toros", Species.VACUNOS);
        l.addCategory("P", "vplantel", "Vacas Plantel", Species.VACUNOS);
        l.addCategory("P", "vgeneral", "Vacas General", Species.VACUNOS);
        l.addCategory("P", "vaqplantel", "Vaquillonas Cria Plantel", Species.VACUNOS);
        l.addCategory("P", "orejanos", "Terneros Orejanos", Species.VACUNOS);
        l.addCategory("P", "vengorde", "Vacas en Engorde", Species.VACUNOS);
        l.addCategory("P", "toroseng", "Toros Engorde", Species.VACUNOS);
        l.addCategory("P", "yeguarizos", "Yeguarizos", Species.YEGUARIZOS);
        l.addEstablishment("LM", "La Marta");
        l.addCategory("LM", "vacas", "Vacas", Species.VACUNOS);

        l.setOpening("P", "toros", 51);
        l.setOpening("P", "vplantel", 37);
        l.setOpening("P", "vgeneral", 658);
        l.setOpening("P", "vaqplantel", 22);
        l.setOpening("P", "orejanos", 818);
        l.setOpening("P", "vengorde", 3);
        l.setOpening("P", "toroseng", 0);
        l.setOpening("P", "yeguarizos", 58);
        l.setOpening("LM", "vacas", 5);
        return l;
    }

    static void cargarMes(Ledger l) {
        l.addAll(List.of(
                Movement.cambioCategoria("m1", sep(5), "P", "toros", "toroseng", 2),
                Movement.cambioCategoria("m2", sep(5), "P", "vplantel", "vengorde", 4),
                Movement.cambioCategoria("m3", sep(5), "P", "vgeneral", "vengorde", 1),
                Movement.cambioCategoria("m4", sep(8), "P", "vaqplantel", "vplantel", 17),
                Movement.nacimiento("m5", sep(10), "P", "orejanos", 29),
                Movement.mortandad("m6", sep(12), "P", "orejanos", 1, 1),
                Movement.venta("m7", sep(20), "P", "vengorde", 3),
                Movement.venta("m8", sep(20), "P", "toroseng", 2)));
    }

    static Planilla.Row fila(Planilla p, String categoryId) {
        return p.rows().stream().filter(r -> r.category().id().equals(categoryId)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("la planilla de septiembre da las existencias finales de la planilla en Excel")
    void laPlanillaDeSeptiembreDaLasExistenciasFinales() {
        Ledger l = escenario();
        cargarMes(l);
        Planilla p = l.planilla("P", SEP);
        eq(49, fila(p, "toros").finalStock(), "Toros");
        eq(50, fila(p, "vplantel").finalStock(), "Vacas Plantel");
        eq(657, fila(p, "vgeneral").finalStock(), "Vacas General");
        eq(5, fila(p, "vaqplantel").finalStock(), "Vaquillonas Cria Plantel");
        eq(846, fila(p, "orejanos").finalStock(), "Terneros Orejanos");
        eq(5, fila(p, "vengorde").finalStock(), "Vacas en Engorde");
        eq(0, fila(p, "toroseng").finalStock(), "Toros Engorde");
    }

    @Test
    @DisplayName("totales de vacunos: 1589 anterior, 1612 final")
    void totalesDeVacunos() {
        Ledger l = escenario();
        cargarMes(l);
        Planilla p = l.planilla("P", SEP);
        Planilla.Totals vac = p.totalFor(Species.VACUNOS);
        eq(1589, vac.anterior(), "anterior vacunos");
        eq(1612, vac.finalStock(), "final vacunos");
        eq(29, vac.columns().get(Column.NACIMIENTO), "nacidos");
        eq(1, vac.columns().get(Column.MORTANDAD), "mortandad");
        eq(5, vac.columns().get(Column.VENTA), "ventas");
        eq(1647, p.totalGeneral().anterior(), "anterior general incluye yeguarizos (1589 + 58)");
    }

    @Test
    @DisplayName("cada fila cumple anterior + entradas - salidas = final")
    void cadaFilaCumpleAnteriorMasEntradasMenosSalidas() {
        Ledger l = escenario();
        cargarMes(l);
        for (Planilla.Row r : l.planilla("P", SEP).rows()) {
            eq(r.finalStock(), r.anterior() + r.entradas() - r.salidas(), r.category().name());
        }
    }

    @Test
    @DisplayName("los cambios de categoria cierran en cero (columna DE = columna A)")
    void losCambiosDeCategoriaCierranEnCero() {
        Ledger l = escenario();
        cargarMes(l);
        Planilla.Totals t = l.planilla("P", SEP).totalGeneral();
        // El escenario carga 2 + 4 + 1 + 17 = 24. La planilla real suma 25 porque tiene una fila mas.
        eq(24, t.columns().get(Column.CAMBIO_SALE), "DE");
        eq(t.columns().get(Column.CAMBIO_SALE), t.columns().get(Column.CAMBIO_ENTRA), "DE igual a A");
    }

    @Test
    @DisplayName("un traslado a La Marta sale de un establecimiento y entra en el otro")
    void unTrasladoSaleDeUnEstablecimientoYEntraEnElOtro() {
        Ledger l = escenario();
        l.add(Movement.traslado("t1", sep(15), "P", "vgeneral", "LM", "vacas", 10));
        Planilla p = l.planilla("P", SEP);
        Planilla lm = l.planilla("LM", SEP);
        eq(10, fila(p, "vgeneral").columns().get(Column.TRASLADO_SALE), "sale de Principal");
        eq(648, fila(p, "vgeneral").finalStock(), "final Principal");
        eq(10, fila(lm, "vacas").columns().get(Column.TRASLADO_ENTRA), "entra en La Marta");
        eq(15, fila(lm, "vacas").finalStock(), "final La Marta");
    }

    @Test
    @DisplayName("la existencia final de un mes es la anterior del siguiente")
    void laExistenciaFinalDeUnMesEsLaAnteriorDelSiguiente() {
        Ledger l = escenario();
        cargarMes(l);
        l.closeMonth("P", SEP);
        l.add(Movement.compra("c1", LocalDate.of(2026, 10, 3), "P", "toros", 4));
        Planilla sept = l.planilla("P", SEP);
        Planilla oct = l.planilla("P", OCT);
        for (Planilla.Row r : sept.rows()) {
            eq(r.finalStock(), fila(oct, r.category().id()).anterior(), r.category().name());
        }
        eq(53, fila(oct, "toros").finalStock(), "Toros octubre");
    }

    @Test
    @DisplayName("rechaza una venta que deja la categoria en negativo")
    void rechazaUnaVentaQueDejaLaCategoriaEnNegativo() {
        Ledger l = escenario();
        rejects("Stock insuficiente", () ->
                l.add(Movement.venta("v1", sep(3), "P", "toroseng", 1)));
        eq(0, l.movements().size(), "no debe quedar guardado");
    }

    @Test
    @DisplayName("rechaza un traslado sin stock y no deja efecto en el destino")
    void rechazaUnTrasladoSinStock() {
        Ledger l = escenario();
        rejects("Stock insuficiente", () ->
                l.add(Movement.traslado("t2", sep(4), "LM", "vacas", "P", "vgeneral", 6)));
        eq(658, l.balance("P", "vgeneral", sep(30)), "destino sin cambios");
    }

    @Test
    @DisplayName("una carga en grilla es todo o nada")
    void unaCargaEnGrillaEsTodoONada() {
        Ledger l = escenario();
        rejects("Stock insuficiente", () -> l.addAll(List.of(
                Movement.nacimiento("g1", sep(2), "P", "orejanos", 10),
                Movement.venta("g2", sep(2), "P", "toroseng", 1))));
        eq(0, l.movements().size(), "no debe quedar ningun movimiento");
    }

    @Test
    @DisplayName("rechaza un movimiento fechado antes del stock inicial")
    void rechazaUnMovimientoFechadoAntesDelStockInicial() {
        Ledger l = escenario();
        rejects("anterior al stock inicial", () ->
                l.add(Movement.compra("x", LocalDate.of(2026, 8, 31), "P", "toros", 1)));
    }

    @Test
    @DisplayName("rechaza movimientos en un mes cerrado, tambien del lado destino de un traslado")
    void rechazaMovimientosEnUnMesCerrado() {
        Ledger l = escenario();
        l.closeMonth("LM", SEP);
        rejects("cerrada", () -> l.add(Movement.compra("x", sep(10), "LM", "vacas", 1)));
        rejects("cerrada", () ->
                l.add(Movement.traslado("x2", sep(10), "P", "toros", "LM", "vacas", 1)));
        eq(0, l.movements().size(), "no debe quedar guardado");
    }

    @Test
    @DisplayName("los meses se cierran en orden y solo se reabre el ultimo")
    void losMesesSeCierranEnOrdenYSoloSeReabreElUltimo() {
        Ledger l = escenario();
        rejects("proximo mes a cerrar", () -> l.closeMonth("P", OCT));
        l.closeMonth("P", SEP);
        l.closeMonth("P", OCT);
        l.reopenLastMonth("P");
        l.add(Movement.compra("c", LocalDate.of(2026, 10, 2), "P", "toros", 1));
        rejects("cerrada", () -> l.add(Movement.compra("d", sep(2), "P", "toros", 1)));
    }

    @Test
    @DisplayName("rechaza una categoria que no es del establecimiento")
    void rechazaUnaCategoriaQueNoEsDelEstablecimiento() {
        Ledger l = escenario();
        rejects("no existe en La Marta", () ->
                l.add(Movement.compra("x", sep(2), "LM", "toros", 1)));
    }

    @Test
    @DisplayName("reglas de forma: cantidad, cueros, cambio a la misma categoria, traslado al mismo lugar")
    void reglasDeForma() {
        rejects("mayor a cero", () -> Movement.compra("a", sep(1), "P", "toros", 0));
        rejects("no pueden superar", () -> Movement.mortandad("b", sep(1), "P", "toros", 1, 2));
        rejects("distinta", () -> Movement.cambioCategoria("c", sep(1), "P", "toros", "toros", 1));
        rejects("otro establecimiento", () -> Movement.traslado("d", sep(1), "P", "toros", "P", "vplantel", 1));
    }

    @Test
    @DisplayName("un movimiento con fecha pasada que dejaria negativo un dia posterior se rechaza")
    void unMovimientoConFechaPasadaQueDejariaNegativoSeRechaza() {
        Ledger l = escenario();
        cargarMes(l);
        // Vacas en Engorde: 3 al inicio, +5 el dia 5, venta 3 el dia 20. Vender 8 el dia 4 deja negativo.
        rejects("Stock insuficiente", () ->
                l.add(Movement.venta("late", sep(4), "P", "vengorde", 4)));
    }

    @Test
    @DisplayName("la vista general filtra por establecimiento, especie y categoria")
    void laVistaGeneralFiltra() {
        Ledger l = escenario();
        cargarMes(l);
        LocalDate fin = LocalDate.of(2026, 9, 30);
        eq(9, l.existencia(fin, null, null, null).size(), "sin filtros");
        eq(1, l.existencia(fin, "LM", null, null).size(), "solo La Marta");
        eq(1, l.existencia(fin, null, Species.YEGUARIZOS, null).size(), "solo yeguarizos");
        eq(846, l.existencia(fin, "P", null, "orejanos").get(0).quantity(), "Terneros Orejanos");
        eq(818, l.existencia(sep(9), "P", null, "orejanos").get(0).quantity(), "antes del nacimiento");
    }
}
