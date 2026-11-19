package com.kinetix.cattlestock.data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Establecimiento identificado por su codigo corto, por ejemplo "P" o "LM". El codigo es unico en toda la
 * instalacion; el panel lo genera a partir del nombre cuando no se escribe uno.
 */
@Entity
@Table(name = "establishment")
public class EstablishmentEntity {

    @Id
    private String code;

    private String name;

    /** Empresa a la que pertenece. */
    private Long companyId;

    protected EstablishmentEntity() {
    }

    public EstablishmentEntity(Long companyId, String code, String name) {
        this.companyId = companyId;
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public void setName(String name) { this.name = name; }
}
