package com.kinetix.cattlestock.data;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.kinetix.cattlestock.domain.Species;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Categoria de animales de una empresa, por ejemplo "Vacas Plantel". El catalogo es de la empresa: todos sus
 * establecimientos pueden usar todas las categorias. El orden de alta (position) es el orden de la planilla.
 */
@Entity
@Table(name = "category", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "code"}))
public class CategoryEntity {

    @Id
    @GeneratedValue
    private Long id;

    private Long companyId;

    private String code;

    private String name;

    // VARCHAR y no ENUM: agregar un valor al enum de Java no obliga a alterar la base
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 30)
    private Species species;

    private int position;

    /** Una categoria deshabilitada no se ofrece para cargar movimientos. Null en las filas viejas: cuenta como habilitada. */
    private Boolean enabled = Boolean.TRUE;

    protected CategoryEntity() {
    }

    public CategoryEntity(Long companyId, String code, String name, Species species, int position) {
        this.companyId = companyId;
        this.code = code;
        this.name = name;
        this.species = species;
        this.position = position;
    }

    public Long getId() { return id; }
    public Long getCompanyId() { return companyId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Species getSpecies() { return species; }
    public int getPosition() { return position; }
    public boolean isEnabled() { return enabled == null || enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setName(String name) { this.name = name; }
    public void setPosition(int position) { this.position = position; }
}
