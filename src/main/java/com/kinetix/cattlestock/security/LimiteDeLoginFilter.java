package com.kinetix.cattlestock.security;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Rechaza el envio del formulario de login cuando el usuario o la direccion superaron los intentos fallidos. */
class LimiteDeLoginFilter extends OncePerRequestFilter {

    private final IntentosDeLogin intentos;

    LimiteDeLoginFilter(IntentosDeLogin intentos) {
        this.intentos = intentos;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && "/login".equals(request.getRequestURI().substring(request.getContextPath().length())));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (intentos.bloqueado(request.getRemoteAddr(), request.getParameter("username"))) {
            response.sendRedirect(request.getContextPath() + "/login?bloqueado");
            return;
        }
        chain.doFilter(request, response);
    }
}
