package com.kinetix.cattlestock.security;

import java.io.IOException;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.kinetix.cattlestock.data.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Mientras el usuario no cambie la clave que le dio el administrador, solo puede ir a cambiarla o salir. */
class CambioDeClaveFilter extends OncePerRequestFilter {

    static final String PAGINA = "/cuenta/clave";

    private final UserRepository users;

    CambioDeClaveFilter(UserRepository users) {
        this.users = users;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        return ruta.equals(PAGINA) || ruta.equals("/logout") || ruta.equals("/login") || ruta.equals("/error")
                || ruta.endsWith(".css") || ruta.endsWith(".js");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)
                && users.findByUsername(auth.getName()).map(u -> u.isMustChangePassword()).orElse(false)) {
            response.sendRedirect(request.getContextPath() + PAGINA);
            return;
        }
        chain.doFilter(request, response);
    }
}
