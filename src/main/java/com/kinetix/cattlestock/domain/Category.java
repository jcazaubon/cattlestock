package com.kinetix.cattlestock.domain;

/** Categoria de animales de un establecimiento, por ejemplo "Vacas Plantel". */
public record Category(String id, String name, Species species) {
}
