package com.kinetix.cattlestock.data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Cierre de meses por establecimiento. closedThrough es el ultimo mes cerrado, en yyyy-MM. */
@Entity
@Table(name = "month_close")
public class MonthCloseEntity {

    @Id
    private String establishmentCode;

    private String closedThrough;

    protected MonthCloseEntity() { }

    public MonthCloseEntity(String establishmentCode, String closedThrough) {
        this.establishmentCode = establishmentCode;
        this.closedThrough = closedThrough;
    }

    public String getEstablishmentCode() { return establishmentCode; }
    public String getClosedThrough() { return closedThrough; }

    public void setClosedThrough(String closedThrough) { this.closedThrough = closedThrough; }
}
