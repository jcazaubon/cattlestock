package com.kinetix.cattlestock.app;

import java.util.Collection;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kinetix.cattlestock.data.EstablishmentRepository;
import com.kinetix.cattlestock.data.UserEntity;
import com.kinetix.cattlestock.data.UserRepository;
import com.kinetix.cattlestock.domain.DomainException;

@Service
public class UserService {

    private final UserRepository users;
    private final EstablishmentRepository establishments;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, EstablishmentRepository establishments, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.establishments = establishments;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void crear(String username, String password, UserEntity.Role role, Collection<String> establishmentCodes) {
        if (username == null || username.isBlank()) {
            throw new DomainException("El usuario necesita nombre");
        }
        if (users.existsByUsername(username)) {
            throw new DomainException("El usuario " + username + " ya existe");
        }
        if (password == null || password.isBlank()) {
            throw new DomainException("El usuario " + username + " necesita password");
        }
        UserEntity u = new UserEntity(username, passwordEncoder.encode(password), role);
        for (String code : establishmentCodes) {
            u.getEstablishments().add(establishments.findById(code)
                    .orElseThrow(() -> new DomainException("El establecimiento " + code + " no existe")));
        }
        users.save(u);
    }
}
