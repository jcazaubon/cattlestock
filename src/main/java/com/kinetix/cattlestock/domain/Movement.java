package com.kinetix.cattlestock.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * Un hecho que cambia la existencia. Un cambio de categoria y un traslado son un solo movimiento
 * con dos lados, para que siempre cierren en cero.
 *
 * establishmentId y categoryId son el origen. destEstablishmentId y destCategoryId solo se usan
 * en traslados (ambos) y en cambios de categoria (solo destCategoryId).
 */
public record Movement(
        String id,
        LocalDate date,
        MovementType type,
        String establishmentId,
        String categoryId,
        int quantity,
        String destEstablishmentId,
        String destCategoryId,
        Integer cueros,
        String note) {

    public Movement {
        if (id == null || id.isBlank()) throw new DomainException("El movimiento necesita un id");
        if (date == null) throw new DomainException("El movimiento necesita fecha");
        if (type == null) throw new DomainException("El movimiento necesita tipo");
        if (establishmentId == null || categoryId == null) {
            throw new DomainException("El movimiento necesita establecimiento y categoria");
        }
        if (quantity <= 0) throw new DomainException("La cantidad debe ser mayor a cero");

        switch (type) {
            case CAMBIO_CATEGORIA -> {
                if (destCategoryId == null || destCategoryId.equals(categoryId)) {
                    throw new DomainException("El cambio de categoria necesita una categoria destino distinta");
                }
                if (destEstablishmentId != null) {
                    throw new DomainException("El cambio de categoria ocurre dentro de un establecimiento");
                }
            }
            case TRASLADO -> {
                if (destEstablishmentId == null || destCategoryId == null) {
                    throw new DomainException("El traslado necesita establecimiento y categoria destino");
                }
                if (destEstablishmentId.equals(establishmentId)) {
                    throw new DomainException("El traslado debe ser a otro establecimiento");
                }
            }
            default -> {
                if (destEstablishmentId != null || destCategoryId != null) {
                    throw new DomainException(type + " no lleva destino");
                }
            }
        }

        if (cueros != null) {
            if (type != MovementType.MORTANDAD) {
                throw new DomainException("Los cueros solo se registran en mortandad");
            }
            if (cueros < 0 || cueros > quantity) {
                throw new DomainException("Los cueros no pueden superar la cantidad de muertes");
            }
        }
    }

    public static Movement compra(String id, LocalDate d, String est, String cat, int q) {
        return new Movement(id, d, MovementType.COMPRA, est, cat, q, null, null, null, null);
    }

    public static Movement venta(String id, LocalDate d, String est, String cat, int q) {
        return new Movement(id, d, MovementType.VENTA, est, cat, q, null, null, null, null);
    }

    public static Movement nacimiento(String id, LocalDate d, String est, String cat, int q) {
        return new Movement(id, d, MovementType.NACIMIENTO, est, cat, q, null, null, null, null);
    }

    public static Movement mortandad(String id, LocalDate d, String est, String cat, int q, int cueros) {
        return new Movement(id, d, MovementType.MORTANDAD, est, cat, q, null, null, cueros, null);
    }

    public static Movement consumo(String id, LocalDate d, String est, String cat, int q) {
        return new Movement(id, d, MovementType.CONSUMO, est, cat, q, null, null, null, null);
    }

    public static Movement cambioCategoria(String id, LocalDate d, String est, String from, String to, int q) {
        return new Movement(id, d, MovementType.CAMBIO_CATEGORIA, est, from, q, null, to, null, null);
    }

    public static Movement traslado(String id, LocalDate d, String fromEst, String fromCat,
                                    String toEst, String toCat, int q) {
        return new Movement(id, d, MovementType.TRASLADO, fromEst, fromCat, q, toEst, toCat, null, null);
    }

    public List<Effect> effects() {
        return switch (type) {
            case COMPRA -> List.of(new Effect(establishmentId, categoryId, date, Column.COMPRA, quantity));
            case NACIMIENTO -> List.of(new Effect(establishmentId, categoryId, date, Column.NACIMIENTO, quantity));
            case VENTA -> List.of(new Effect(establishmentId, categoryId, date, Column.VENTA, quantity));
            case MORTANDAD -> List.of(new Effect(establishmentId, categoryId, date, Column.MORTANDAD, quantity));
            case CONSUMO -> List.of(new Effect(establishmentId, categoryId, date, Column.CONSUMO, quantity));
            case CAMBIO_CATEGORIA -> List.of(
                    new Effect(establishmentId, categoryId, date, Column.CAMBIO_SALE, quantity),
                    new Effect(establishmentId, destCategoryId, date, Column.CAMBIO_ENTRA, quantity));
            case TRASLADO -> List.of(
                    new Effect(establishmentId, categoryId, date, Column.TRASLADO_SALE, quantity),
                    new Effect(destEstablishmentId, destCategoryId, date, Column.TRASLADO_ENTRA, quantity));
        };
    }
}
