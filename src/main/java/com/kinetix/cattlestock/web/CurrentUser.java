package com.kinetix.cattlestock.web;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.kinetix.cattlestock.data.EstablishmentEntity;
import com.kinetix.cattlestock.data.EstablishmentRepository;
import com.kinetix.cattlestock.data.UserEntity;
import com.kinetix.cattlestock.data.UserRepository;

/** El usuario logueado y lo que puede ver. */
@Component
public class CurrentUser {

    private final UserRepository users;
    private final EstablishmentRepository establishments;

    public CurrentUser(UserRepository users, EstablishmentRepository establishments) {
        this.users = users;
        this.establishments = establishments;
    }

    public Optional<UserEntity> get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return users.findByUsername(auth.getName());
    }

    public boolean esAdmin() {
        return get().map(u -> u.getRole() == UserEntity.Role.ADMIN).orElse(false);
    }

    /** Establecimientos que el usuario puede ver y cargar. El administrador ve todos. */
    public List<EstablishmentEntity> establecimientos() {
        Optional<UserEntity> u = get();
        if (u.isEmpty()) return List.of();
        if (u.get().getRole() == UserEntity.Role.ADMIN) {
            return establishments.findAllByOrderByCodeAsc();
        }
        return u.get().getEstablishments().stream()
                .sorted(Comparator.comparing(EstablishmentEntity::getCode))
                .toList();
    }

    public boolean puedeAcceder(String establishmentCode) {
        return establecimientos().stream().anyMatch(e -> e.getCode().equals(establishmentCode));
    }
}
