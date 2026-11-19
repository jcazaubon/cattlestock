package com.kinetix.cattlestock.data;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Stock inicial de una categoria de un establecimiento, al 1 del mes de arranque. */
@Entity
@Table(name = "opening", uniqueConstraints = @UniqueConstraint(columnNames = {"establishment_code", "category_code"}))
public class OpeningEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String establishmentCode;

    private String categoryCode;

    private int quantity;

    protected OpeningEntity() { }

    public OpeningEntity(String establishmentCode, String categoryCode) {
        this.establishmentCode = establishmentCode;
        this.categoryCode = categoryCode;
    }

    public Long getId() { return id; }
    public String getEstablishmentCode() { return establishmentCode; }
    public String getCategoryCode() { return categoryCode; }
    public int getQuantity() { return quantity; }

    public void setQuantity(int quantity) { this.quantity = quantity; }
}
