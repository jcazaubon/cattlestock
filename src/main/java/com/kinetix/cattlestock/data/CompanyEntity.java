package com.kinetix.cattlestock.data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Empresa. La instalacion puede tener varias, cada una con sus establecimientos, categorias y usuarios. */
@Entity
@Table(name = "company")
public class CompanyEntity {

    @Id
    private Long id;

    private String name;

    /** Mes cuyo "existencia anterior" es el stock inicial, en formato yyyy-MM. */
    private String startMonth;

    protected CompanyEntity() {
    }

    public CompanyEntity(Long id, String name, String startMonth) {
        this.id = id;
        this.name = name;
        this.startMonth = startMonth;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getStartMonth() { return startMonth; }
    public void setName(String name) { this.name = name; }
}
