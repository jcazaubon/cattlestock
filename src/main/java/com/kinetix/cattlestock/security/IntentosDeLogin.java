package com.kinetix.cattlestock.security;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Frena la fuerza bruta sobre el login: despues de varios intentos fallidos seguidos de un usuario (o desde una
 * misma direccion) se rechazan los ingresos durante un rato. Vive en memoria: con mas de un servidor hay que
 * llevarlo a una base compartida.
 */
@Component
public class IntentosDeLogin {

    static final int MAXIMO_POR_USUARIO = 5;
    static final int MAXIMO_POR_DIRECCION = 20;
    static final Duration VENTANA = Duration.ofMinutes(15);

    private final Map<String, ArrayDeque<Long>> fallos = new ConcurrentHashMap<>();

    public boolean bloqueado(String direccion, String usuario) {
        return cuenta("u:" + clave(usuario)) >= MAXIMO_POR_USUARIO || cuenta("d:" + direccion) >= MAXIMO_POR_DIRECCION;
    }

    public void fallo(String direccion, String usuario) {
        registrar("u:" + clave(usuario));
        registrar("d:" + direccion);
    }

    public void exito(String usuario) {
        fallos.remove("u:" + clave(usuario));
    }

    private static String clave(String usuario) {
        return usuario == null ? "" : usuario.trim().toLowerCase(Locale.ROOT);
    }

    private int cuenta(String clave) {
        ArrayDeque<Long> l = fallos.get(clave);
        if (l == null) return 0;
        synchronized (l) {
            purgar(l);
            return l.size();
        }
    }

    private void registrar(String clave) {
        ArrayDeque<Long> l = fallos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (l) {
            purgar(l);
            l.addLast(System.currentTimeMillis());
        }
    }

    private static void purgar(ArrayDeque<Long> l) {
        long limite = System.currentTimeMillis() - VENTANA.toMillis();
        while (!l.isEmpty() && l.peekFirst() < limite) l.pollFirst();
    }
}
