package com.kinetix.cattlestock.data;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Usuario de la app con su rol, su empresa y los establecimientos asignados. */
@Entity
@Table(name = "app_user", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
public class UserEntity {

    /**
     * SUPERADMIN ve todas las empresas y no pertenece a ninguna; ADMIN administra solo la suya;
     * CARGA registra movimientos en los establecimientos que tiene asignados.
     */
    public enum Role { SUPERADMIN, ADMIN, CARGA }

    @Id
    @GeneratedValue
    private Long id;

    private String username;

    /** Hash BCrypt, nunca el password en claro. */
    private String password;

    // VARCHAR y no ENUM: agregar un valor al enum de Java no obliga a alterar la base
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 30)
    private Role role;

    /** Verdadero mientras el usuario usa la clave que le dio el administrador: tiene que cambiarla al entrar. */
    private Boolean mustChangePassword = Boolean.FALSE;

    /** Empresa del usuario. Null para el superadmin. */
    private Long companyId;

    @ManyToMany(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinTable(name = "user_establishment",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "establishment_code"))
    private Set<EstablishmentEntity> establishments = new LinkedHashSet<>();

    protected UserEntity() {
    }

    public UserEntity(String username, String password, Role role, Long companyId) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.companyId = companyId;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isMustChangePassword() { return Boolean.TRUE.equals(mustChangePassword); }
    public void setMustChangePassword(boolean value) { this.mustChangePassword = value; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Set<EstablishmentEntity> getEstablishments() { return establishments; }
}
