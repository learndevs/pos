package com.retailpos.server.sync.server;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A built configuration package (the zip) and how it was scoped. */
@Entity
@Table(name = "config_package")
public class ConfigPackage {

    @Id
    @Column(name = "package_no")
    private Integer packageNo;

    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "created_by") private UUID createdBy;
    private String note;
    @Column(nullable = false) private String scope;              // FULL | DATE_RANGE
    @Column(name = "date_from") private LocalDate dateFrom;
    @Column(name = "date_to") private LocalDate dateTo;
    @Column(nullable = false) private String entities;           // TAX_RATE,CATEGORY,PRODUCT
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "item_counts", nullable = false, columnDefinition = "jsonb") private String itemCounts;
    @Column(nullable = false) private String sha256;
    @Column(name = "size_bytes", nullable = false) private int sizeBytes;
    @Column(nullable = false, columnDefinition = "bytea") private byte[] zip;

    protected ConfigPackage() {}

    ConfigPackage(int packageNo, Instant createdAt, UUID createdBy, String note, String scope, LocalDate dateFrom,
                  LocalDate dateTo, String entities, String itemCounts, String sha256, byte[] zip) {
        this.packageNo = packageNo;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.note = note;
        this.scope = scope;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.entities = entities;
        this.itemCounts = itemCounts;
        this.sha256 = sha256;
        this.sizeBytes = zip.length;
        this.zip = zip;
    }

    public Integer getPackageNo() { return packageNo; }
    public Instant getCreatedAt() { return createdAt; }
    public String getNote() { return note; }
    public String getScope() { return scope; }
    public LocalDate getDateFrom() { return dateFrom; }
    public LocalDate getDateTo() { return dateTo; }
    public String getEntities() { return entities; }
    public String getItemCounts() { return itemCounts; }
    public String getSha256() { return sha256; }
    public int getSizeBytes() { return sizeBytes; }
    public byte[] getZip() { return zip; }
}
