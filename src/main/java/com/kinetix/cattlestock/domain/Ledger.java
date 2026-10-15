package com.kinetix.cattlestock.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Libro de movimientos de una empresa. El stock no se guarda: es el stock inicial mas los movimientos.
 *
 * Reglas:
 * - Ninguna categoria puede quedar en negativo al cierre de ningun dia.
 * - Un mes cerrado no admite movimientos, en ninguno de los dos lados de un traslado.
 * - Los meses de un establecimiento se cierran en orden, y solo se reabre el ultimo cerrado.
 */
public final class Ledger {

    /** Fila de la vista general de existencia. */
    public record ExistenceRow(Establishment establishment, Category category, int quantity) {
    }

    private final String companyName;
    private final YearMonth startMonth;
    private final Map<String, Establishment> establishments = new LinkedHashMap<>();
    private final Map<String, Integer> opening = new HashMap<>();
    private final List<Movement> movements = new ArrayList<>();
    private final Map<String, YearMonth> closedThrough = new HashMap<>();

    /** startMonth es el mes cuyo "existencia anterior" es el stock inicial. */
    public Ledger(String companyName, YearMonth startMonth) {
        this.companyName = companyName;
        this.startMonth = startMonth;
    }

    static String key(String establishmentId, String categoryId) {
        return establishmentId + "|" + categoryId;
    }

    public String companyName() { return companyName; }
    public YearMonth startMonth() { return startMonth; }

    // --- Configuracion ---

    public Establishment addEstablishment(String id, String name) {
        Establishment e = new Establishment(id, name);
        if (establishments.putIfAbsent(id, e) != null) {
            throw new DomainException("El establecimiento " + id + " ya existe");
        }
        return e;
    }

    public Category addCategory(String establishmentId, String categoryId, String name, Species species) {
        Category c = new Category(categoryId, name, species);
        establishment(establishmentId).addCategory(c);
        return c;
    }

    public Establishment establishment(String id) {
        Establishment e = establishments.get(id);
        if (e == null) throw new DomainException("El establecimiento " + id + " no existe");
        return e;
    }

    public List<Establishment> establishments() {
        return List.copyOf(establishments.values());
    }

    public List<Movement> movements() {
        return Collections.unmodifiableList(movements);
    }

    // --- Stock inicial ---

    public void setOpening(String establishmentId, String categoryId, int quantity) {
        establishment(establishmentId).category(categoryId);
        if (quantity < 0) throw new DomainException("El stock inicial no puede ser negativo");
        if (isClosed(establishmentId, startMonth.atDay(1))) {
            throw new DomainException("El mes inicial ya esta cerrado");
        }
        String k = key(establishmentId, categoryId);
        Integer previous = opening.put(k, quantity);
        try {
            checkNonNegative(Set.of(k));
        } catch (DomainException ex) {
            if (previous == null) opening.remove(k); else opening.put(k, previous);
            throw ex;
        }
    }

    // --- Movimientos ---

    public void add(Movement m) {
        validateReferences(m);
        for (Movement existing : movements) {
            if (existing.id().equals(m.id())) throw new DomainException("Ya existe el movimiento " + m.id());
        }
        for (Effect e : m.effects()) {
            if (isClosed(e.establishmentId(), e.date())) {
                throw new DomainException("La planilla de " + establishment(e.establishmentId()).name()
                        + " de " + YearMonth.from(e.date()) + " esta cerrada");
            }
        }
        movements.add(m);
        Set<String> keys = new HashSet<>();
        for (Effect e : m.effects()) keys.add(e.key());
        try {
            checkNonNegative(keys);
        } catch (DomainException ex) {
            movements.remove(movements.size() - 1);
            throw ex;
        }
    }

    /** Agrega todos o ninguno. Sirve para la carga en grilla. */
    public void addAll(List<Movement> batch) {
        int before = movements.size();
        try {
            for (Movement m : batch) add(m);
        } catch (DomainException ex) {
            while (movements.size() > before) movements.remove(movements.size() - 1);
            throw ex;
        }
    }

    private void validateReferences(Movement m) {
        if (YearMonth.from(m.date()).isBefore(startMonth)) {
            throw new DomainException("La fecha es anterior al stock inicial (" + startMonth + ")");
        }
        establishment(m.establishmentId()).category(m.categoryId());
        switch (m.type()) {
            case CAMBIO_CATEGORIA -> establishment(m.establishmentId()).category(m.destCategoryId());
            case TRASLADO -> establishment(m.destEstablishmentId()).category(m.destCategoryId());
            default -> { }
        }
    }

    // --- Existencia ---

    public int balance(String establishmentId, String categoryId, LocalDate asOf) {
        int total = opening.getOrDefault(key(establishmentId, categoryId), 0);
        String k = key(establishmentId, categoryId);
        for (Movement m : movements) {
            for (Effect e : m.effects()) {
                if (e.key().equals(k) && !e.date().isAfter(asOf)) total += e.signed();
            }
        }
        return total;
    }

    /** Vista general de existencia a una fecha. Los filtros en null no filtran. */
    public List<ExistenceRow> existencia(LocalDate asOf, String establishmentId, Species species, String categoryId) {
        List<ExistenceRow> out = new ArrayList<>();
        for (Establishment est : establishments.values()) {
            if (establishmentId != null && !est.id().equals(establishmentId)) continue;
            for (Category c : est.categories()) {
                if (species != null && c.species() != species) continue;
                if (categoryId != null && !c.id().equals(categoryId)) continue;
                out.add(new ExistenceRow(est, c, balance(est.id(), c.id(), asOf)));
            }
        }
        return out;
    }

    // --- Planilla mensual ---

    public Planilla planilla(String establishmentId, YearMonth month) {
        if (month.isBefore(startMonth)) throw new DomainException("El mes es anterior al stock inicial");
        Establishment est = establishment(establishmentId);
        LocalDate first = month.atDay(1);
        List<Planilla.Row> rows = new ArrayList<>();
        for (Category c : est.categories()) {
            String k = key(est.id(), c.id());
            int anterior = opening.getOrDefault(k, 0);
            Map<Column, Integer> columns = new EnumMap<>(Column.class);
            for (Column col : Column.values()) columns.put(col, 0);
            for (Movement m : movements) {
                for (Effect e : m.effects()) {
                    if (!e.key().equals(k)) continue;
                    if (e.date().isBefore(first)) {
                        anterior += e.signed();
                    } else if (YearMonth.from(e.date()).equals(month)) {
                        columns.merge(e.column(), e.quantity(), Integer::sum);
                    }
                }
            }
            int finalStock = anterior;
            for (Map.Entry<Column, Integer> e : columns.entrySet()) {
                finalStock += e.getKey().entrada ? e.getValue() : -e.getValue();
            }
            rows.add(new Planilla.Row(c, anterior, columns, finalStock));
        }
        return new Planilla(est, month, isClosed(establishmentId, first), rows);
    }

    // --- Cierre de mes ---

    public boolean isClosed(String establishmentId, LocalDate date) {
        YearMonth through = closedThrough.get(establishmentId);
        return through != null && !YearMonth.from(date).isAfter(through);
    }

    public void closeMonth(String establishmentId, YearMonth month) {
        establishment(establishmentId);
        YearMonth through = closedThrough.get(establishmentId);
        YearMonth expected = through == null ? startMonth : through.plusMonths(1);
        if (!month.equals(expected)) {
            throw new DomainException("El proximo mes a cerrar es " + expected);
        }
        closedThrough.put(establishmentId, month);
    }

    /** Reabre el ultimo mes cerrado. Quien la llame debe verificar que el usuario es administrador. */
    public void reopenLastMonth(String establishmentId) {
        establishment(establishmentId);
        YearMonth through = closedThrough.get(establishmentId);
        if (through == null) throw new DomainException("No hay meses cerrados");
        if (through.equals(startMonth)) closedThrough.remove(establishmentId);
        else closedThrough.put(establishmentId, through.minusMonths(1));
    }

    // --- Validacion de stock negativo ---

    /** Revisa, por cada clave, que el saldo no sea negativo al cierre de ningun dia. */
    private void checkNonNegative(Set<String> keys) {
        for (String k : keys) {
            TreeMap<LocalDate, Integer> deltaByDay = new TreeMap<>();
            for (Movement m : movements) {
                for (Effect e : m.effects()) {
                    if (e.key().equals(k)) deltaByDay.merge(e.date(), e.signed(), Integer::sum);
                }
            }
            int running = opening.getOrDefault(k, 0);
            if (running < 0) throw new DomainException("Stock negativo en " + k);
            for (Map.Entry<LocalDate, Integer> day : deltaByDay.entrySet()) {
                running += day.getValue();
                if (running < 0) {
                    String[] parts = k.split("\\|");
                    throw new DomainException("Stock insuficiente en " + establishment(parts[0]).category(parts[1]).name()
                            + " de " + establishment(parts[0]).name() + " al " + day.getKey()
                            + ": quedaria en " + running);
                }
            }
        }
    }
}
