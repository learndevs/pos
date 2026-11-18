package com.retailpos.server.sync.server;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface ChangeRecordRepository extends JpaRepository<ChangeRecord, Long> {

    @Query("""
        SELECT DISTINCT c.entityId FROM ChangeRecord c
        WHERE c.entity = :entity AND c.createdAt >= :from AND c.createdAt < :to""")
    List<UUID> changedIds(com.retailpos.server.sync.contract.Contract.Entity entity, Instant from, Instant to);
}

interface TerminalRepository extends JpaRepository<Terminal, UUID> {
    Optional<Terminal> findByTokenSha256AndActiveTrue(String tokenSha256);
    List<Terminal> findAllByOrderByNameAsc();
}

interface ConfigPackageRepository extends JpaRepository<ConfigPackage, Integer> {

    @Query(value = "SELECT nextval('config_package_package_no_seq')", nativeQuery = true)
    int nextPackageNo();

    List<ConfigPackage> findTop50ByOrderByPackageNoDesc();
}

interface PackageTargetRepository extends JpaRepository<PackageTarget, PackageTarget.Key> {

    List<PackageTarget> findByPackageNoOrderByTerminalId(int packageNo);

    @Query("""
        SELECT t FROM PackageTarget t WHERE t.terminalId = :terminalId AND t.status <> 'APPLIED'
        ORDER BY t.packageNo""")
    List<PackageTarget> waitingFor(UUID terminalId);

    /** Newest package this till applied: [packageNo, createdAt] or empty. */
    @Query(value = """
        SELECT p.package_no, p.created_at FROM package_target t JOIN config_package p ON p.package_no = t.package_no
        WHERE t.terminal_id = :terminalId AND t.status = 'APPLIED' ORDER BY p.package_no DESC LIMIT 1""", nativeQuery = true)
    List<Object[]> lastApplied(UUID terminalId);
}

interface ArchiveRepository extends JpaRepository<ArchivedTransaction, UUID> {

    @Modifying
    @Query(value = """
        INSERT INTO transaction_archive (id, terminal_id, txn_no, receipt_no, status, business_date, total_minor, finished_at, payload)
        VALUES (:id, :terminalId, :txnNo, :receiptNo, :status, :businessDate, :totalMinor, :finishedAt, CAST(:payload AS jsonb))
        ON CONFLICT (id) DO NOTHING""", nativeQuery = true)
    int insertIfAbsent(UUID id, UUID terminalId, long txnNo, String receiptNo, String status,
                       java.time.LocalDate businessDate, long totalMinor, Instant finishedAt, String payload);

    @Query("""
        SELECT a FROM ArchivedTransaction a
        WHERE (:terminalId IS NULL OR a.terminalId = :terminalId)
          AND (CAST(:from AS LocalDate) IS NULL OR a.businessDate >= :from)
          AND (CAST(:to AS LocalDate) IS NULL OR a.businessDate <= :to)
          AND (:q IS NULL OR a.receiptNo LIKE concat('%', :q, '%') OR CAST(a.txnNo AS string) = :q)
        ORDER BY a.businessDate DESC, a.receivedAt DESC""")
    List<ArchivedTransaction> search(UUID terminalId, java.time.LocalDate from, java.time.LocalDate to, String q,
                                     org.springframework.data.domain.Pageable page);
}
