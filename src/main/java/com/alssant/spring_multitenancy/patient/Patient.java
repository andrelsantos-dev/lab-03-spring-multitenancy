package com.alssant.spring_multitenancy.patient;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "patients")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(insertable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", insertable = false, updatable = false)
    private UUID tenantId;

    private String name;

    public Patient() {
    }

    public Patient(String name) {
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
