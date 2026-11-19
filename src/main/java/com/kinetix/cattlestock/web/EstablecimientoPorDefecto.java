package com.kinetix.cattlestock.web;

import java.util.List;

import com.kinetix.cattlestock.data.EstablishmentEntity;

import jakarta.servlet.http.HttpSession;

/**
 * El establecimiento con el que arrancan las pantallas de carga, para no empezar con un campo vacio: el ultimo en el
 * que la persona cargo movimientos en esta sesion, si todavia es uno de los suyos, o el unico que tiene.
 */
final class EstablecimientoPorDefecto {

    private static final String CLAVE = "ultimoEstablecimiento";

    private EstablecimientoPorDefecto() {
    }

    static String elegir(List<EstablishmentEntity> disponibles, HttpSession sesion) {
        Object ultimo = sesion == null ? null : sesion.getAttribute(CLAVE);
        if (ultimo instanceof String codigo && disponibles.stream().anyMatch(e -> e.getCode().equals(codigo))) {
            return codigo;
        }
        return disponibles.size() == 1 ? disponibles.get(0).getCode() : "";
    }

    static void recordar(HttpSession sesion, String codigo) {
        if (sesion != null && codigo != null && !codigo.isBlank()) sesion.setAttribute(CLAVE, codigo);
    }
}
