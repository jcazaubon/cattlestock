package com.kinetix.cattlestock.domain;

/** Error de regla de negocio. La capa web lo convierte en un mensaje para el usuario. */
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
