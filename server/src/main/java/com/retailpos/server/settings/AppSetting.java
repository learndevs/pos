package com.retailpos.server.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Key/value configuration (business, receipt, printer, discount, license). Value is JSON. */
@Entity
@Table(name = "app_setting")
public class AppSetting {

    @Id
    private String key;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String value;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppSetting() {}

    public String getKey() { return key; }
    public String getValue() { return value; }
}
