package com.retailpos.server.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "role")
public class Role {

    public static final String OWNER = "OWNER";

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    protected Role() {}

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
}
