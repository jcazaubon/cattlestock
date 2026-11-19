package com.kinetix.cattlestock.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.kinetix.cattlestock.data.UserRepository;

/**
 * Login propio, todo autenticado. Configuracion para administradores de empresa y superadmin; /admin solo para el superadmin.
 * El usuario de rol CARGA solo ve y carga en sus establecimientos (se controla en los controllers).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UserRepository users, IntentosDeLogin intentos)
            throws Exception {
        http
                .addFilterBefore(new LimiteDeLoginFilter(intentos),
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new CambioDeClaveFilter(users),
                        org.springframework.security.web.access.intercept.AuthorizationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login").permitAll()
                        .requestMatchers("/htmx.min.js").permitAll()
                        .requestMatchers("/app.css").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/h2-console/**").hasRole("SUPERADMIN")
                        .requestMatchers("/admin/**").hasRole("SUPERADMIN")
                        .requestMatchers("/historial/**").hasAnyRole("ADMIN", "SUPERADMIN")
                        .requestMatchers("/configuracion/**").hasAnyRole("ADMIN", "SUPERADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(alEntrar(intentos))
                        .failureHandler(alFallar(intentos))
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self'; "
                                + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                                + "font-src 'self' https://fonts.gstatic.com; img-src 'self' data:; "
                                + "object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'self'"))
                        .referrerPolicy(ref -> ref.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
                                        .ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));
        return http.build();
    }

    /** Entrar bien borra los fallos del usuario y lleva a donde queria ir (o al inicio). */
    private static org.springframework.security.web.authentication.AuthenticationSuccessHandler alEntrar(
            IntentosDeLogin intentos) {
        var base = new org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler();
        base.setDefaultTargetUrl("/");
        return (request, response, authentication) -> {
            intentos.exito(authentication.getName());
            base.onAuthenticationSuccess(request, response, authentication);
        };
    }

    /** Un intento fallido cuenta contra el usuario y la direccion, y vuelve al login con el aviso. */
    private static org.springframework.security.web.authentication.AuthenticationFailureHandler alFallar(
            IntentosDeLogin intentos) {
        return (request, response, excepcion) -> {
            intentos.fallo(request.getRemoteAddr(), request.getParameter("username"));
            response.sendRedirect(request.getContextPath() + "/login?error");
        };
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository users) {
        return username -> users.findByUsername(username)
                .map(u -> org.springframework.security.core.userdetails.User
                        .withUsername(u.getUsername())
                        .password(u.getPassword())
                        .roles(u.getRole().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
