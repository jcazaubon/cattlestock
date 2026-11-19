package com.kinetix.cattlestock.web;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.kinetix.cattlestock.data.CompanyEntity;
import com.kinetix.cattlestock.data.CompanyRepository;
import com.kinetix.cattlestock.data.EstablishmentEntity;
import com.kinetix.cattlestock.data.EstablishmentRepository;
import com.kinetix.cattlestock.data.UserEntity;
import com.kinetix.cattlestock.data.UserRepository;

/**
 * El usuario logueado y lo que puede ver. El administrador y el de carga trabajan en su empresa; el
 * superadmin ve todas y trabaja en la que eligio (queda en la sesion), por defecto la primera.
 */
@Component
public class CurrentUser {

    static final String SESION_EMPRESA = "empresaActiva";

    private final UserRepository users;
    private final EstablishmentRepository establishments;
    private final CompanyRepository companies;

    public CurrentUser(UserRepository users, EstablishmentRepository establishments, CompanyRepository companies) {
        this.users = users;
        this.establishments = establishments;
        this.companies = companies;
    }

    public Optional<UserEntity> get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return users.findByUsername(auth.getName());
    }

    public boolean esSuperadmin() {
        return get().map(u -> u.getRole() == UserEntity.Role.SUPERADMIN).orElse(false);
    }

    /** Puede configurar: el administrador de su empresa y el superadmin. */
    public boolean esAdmin() {
        return get().map(u -> u.getRole() == UserEntity.Role.ADMIN || u.getRole() == UserEntity.Role.SUPERADMIN)
                .orElse(false);
    }

    /** La empresa en la que trabaja el usuario ahora. */
    public Optional<Long> empresaId() {
        Optional<UserEntity> u = get();
        if (u.isEmpty()) return Optional.empty();
        if (u.get().getRole() != UserEntity.Role.SUPERADMIN) {
            return Optional.ofNullable(u.get().getCompanyId());
        }
        Object elegida = sesion(false) == null ? null : sesion(false).getAttribute(SESION_EMPRESA);
        if (elegida instanceof Long id && companies.existsById(id)) return Optional.of(id);
        return companies.findAll().stream().map(CompanyEntity::getId).min(Comparator.naturalOrder());
    }

    public Optional<CompanyEntity> empresa() {
        return empresaId().flatMap(companies::findById);
    }

    /** Las empresas que el usuario puede ver: todas para el superadmin, la suya para el resto. */
    public List<CompanyEntity> empresas() {
        if (esSuperadmin()) return companies.findAllByOrderByNameAsc();
        return empresa().map(List::of).orElse(List.of());
    }

    /** El superadmin cambia de empresa de trabajo. */
    public void elegirEmpresa(Long id) {
        if (esSuperadmin() && companies.existsById(id) && sesion(true) != null) {
            sesion(true).setAttribute(SESION_EMPRESA, id);
        }
    }

    /**
     * Los establecimientos en los que el usuario opera (planillas, existencia, carga de movimientos): los que
     * tiene asignados. Un administrador sin ninguno asignado ve todos los de su empresa, para que uno nuevo no
     * quede sin nada; el superadmin ve todos los de la empresa en la que trabaja.
     */
    public List<EstablishmentEntity> establecimientos() {
        Optional<UserEntity> u = get();
        if (u.isEmpty()) return List.of();
        UserEntity user = u.get();
        if (user.getRole() == UserEntity.Role.SUPERADMIN) return establecimientosDeLaEmpresa();
        List<EstablishmentEntity> asignados = user.getEstablishments().stream()
                .filter(e -> java.util.Objects.equals(e.getCompanyId(), user.getCompanyId()))
                .sorted(Comparator.comparing(EstablishmentEntity::getCode))
                .toList();
        if (user.getRole() == UserEntity.Role.ADMIN && asignados.isEmpty()) return establecimientosDeLaEmpresa();
        return asignados;
    }

    /** Todos los establecimientos de la empresa en la que trabaja el usuario: los que un administrador configura. */
    public List<EstablishmentEntity> establecimientosDeLaEmpresa() {
        return empresaId().map(establishments::findByCompanyIdOrderByCodeAsc).orElse(List.of());
    }

    public boolean perteneceALaEmpresa(String establishmentCode) {
        return establecimientosDeLaEmpresa().stream().anyMatch(e -> e.getCode().equals(establishmentCode));
    }

    public boolean puedeAcceder(String establishmentCode) {
        return establecimientos().stream().anyMatch(e -> e.getCode().equals(establishmentCode));
    }

    private static jakarta.servlet.http.HttpSession sesion(boolean crear) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes sra) {
            return sra.getRequest().getSession(crear);
        }
        return null;
    }
}
