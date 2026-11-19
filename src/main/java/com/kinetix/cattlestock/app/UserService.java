package com.kinetix.cattlestock.app;

import java.util.Collection;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kinetix.cattlestock.data.CompanyRepository;
import com.kinetix.cattlestock.data.EstablishmentEntity;
import com.kinetix.cattlestock.data.EstablishmentRepository;
import com.kinetix.cattlestock.data.UserEntity;
import com.kinetix.cattlestock.data.UserRepository;
import com.kinetix.cattlestock.domain.DomainException;

/** Altas de usuarios y asignacion de roles y establecimientos. */
@Service
public class UserService {

    private final UserRepository users;
    private final EstablishmentRepository establishments;
    private final CompanyRepository companies;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, EstablishmentRepository establishments,
                       CompanyRepository companies, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.establishments = establishments;
        this.companies = companies;
        this.passwordEncoder = passwordEncoder;
    }

    /** Crea un superadmin: no pertenece a ninguna empresa y las ve todas. */
    @Transactional
    public void crearSuperadmin(String username, String password) {
        crear(username, password, UserEntity.Role.SUPERADMIN, null, List.of());
    }

    /**
     * Crea un usuario. El superadmin no lleva empresa; el administrador y el de carga pertenecen a una
     * empresa, y sus establecimientos tienen que ser de esa misma empresa.
     */
    @Transactional
    public void crear(String username, String password, UserEntity.Role role, Long companyId,
                      Collection<String> establishmentCodes) {
        if (username == null || username.isBlank()) {
            throw new DomainException("El usuario necesita nombre");
        }
        if (users.existsByUsername(username)) {
            throw new DomainException("El usuario " + username + " ya existe");
        }
        if (password == null || password.isBlank()) {
            throw new DomainException("El usuario " + username + " necesita password");
        }
        if (role == null) {
            throw new DomainException("El usuario " + username + " necesita rol");
        }
        if (role == UserEntity.Role.SUPERADMIN) {
            companyId = null;
        } else if (companyId == null || !companies.existsById(companyId)) {
            throw new DomainException("El usuario " + username + " necesita una empresa");
        }
        UserEntity u = new UserEntity(username, passwordEncoder.encode(password), role, companyId);
        asignar(u, establishmentCodes);
        users.save(u);
    }

    /** Cambia el rol y los establecimientos de un usuario de la empresa indicada. */
    @Transactional
    public void actualizar(Long userId, Long companyId, UserEntity.Role role, Collection<String> establishmentCodes) {
        UserEntity u = users.findById(userId).orElseThrow(() -> new DomainException("El usuario no existe"));
        if (u.getRole() == UserEntity.Role.SUPERADMIN || !companyId.equals(u.getCompanyId())) {
            throw new DomainException("Ese usuario no es de tu empresa");
        }
        if (role == null || role == UserEntity.Role.SUPERADMIN) {
            throw new DomainException("El rol no es valido");
        }
        if (u.getRole() == UserEntity.Role.ADMIN && role != UserEntity.Role.ADMIN
                && users.findByCompanyIdOrderByUsernameAsc(companyId).stream()
                        .filter(x -> x.getRole() == UserEntity.Role.ADMIN).count() <= 1) {
            throw new DomainException("La empresa necesita al menos un administrador");
        }
        u.setRole(role);
        asignar(u, establishmentCodes);
        users.save(u);
    }

    private void asignar(UserEntity u, Collection<String> establishmentCodes) {
        u.getEstablishments().clear();
        for (String code : establishmentCodes) {
            EstablishmentEntity est = establishments.findById(code)
                    .orElseThrow(() -> new DomainException("El establecimiento " + code + " no existe"));
            if (u.getCompanyId() == null || !u.getCompanyId().equals(est.getCompanyId())) {
                throw new DomainException("El establecimiento " + code + " no es de la empresa del usuario");
            }
            u.getEstablishments().add(est);
        }
    }
}
