package com.retailpos.server.sync.server;

import com.retailpos.server.catalog.Category;
import com.retailpos.server.catalog.CategoryRepository;
import com.retailpos.server.catalog.Product;
import com.retailpos.server.catalog.ProductRepository;
import com.retailpos.server.catalog.TaxRate;
import com.retailpos.server.catalog.TaxRateRepository;
import com.retailpos.server.common.PosException;
import com.retailpos.server.sync.contract.CatalogMapper;
import com.retailpos.server.sync.contract.Contract;
import com.retailpos.server.sync.contract.Contract.Entity;
import com.retailpos.server.sync.contract.Contract.Manifest;
import com.retailpos.server.sync.contract.Contract.PackageContent;
import com.retailpos.server.sync.contract.Contract.PackageInfo;
import com.retailpos.server.sync.contract.PackageZip;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Builds configuration packages (zip) from the current master data, scoped by entities + date range (or full),
 * targeted at chosen tills; serves them to tills and records their APPLIED / FAILED acknowledgements.
 */
@Service
@Transactional
public class PackageService {

    public enum Scope { FULL, DATE_RANGE }

    public record BuildRequest(Set<Entity> entities, Scope scope, LocalDate dateFrom, LocalDate dateTo,
                               List<UUID> terminalIds, String note) {}

    public record TargetView(UUID terminalId, String terminalName, String status, String error, Instant updatedAt) {}

    public record PackageView(int packageNo, Instant createdAt, String scope, LocalDate dateFrom, LocalDate dateTo,
                              List<Entity> entities, Map<String, Integer> counts, int sizeBytes, String sha256,
                              String note, List<TargetView> targets, List<String> warnings) {}

    private final ConfigPackageRepository packages;
    private final PackageTargetRepository targets;
    private final ChangeLog changeLog;
    private final Terminals terminals;
    private final TaxRateRepository taxRates;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final ObjectMapper json;
    private final Clock clock;

    PackageService(ConfigPackageRepository packages, PackageTargetRepository targets, ChangeLog changeLog, Terminals terminals,
                   TaxRateRepository taxRates, CategoryRepository categories, ProductRepository products,
                   ObjectMapper json, Clock clock) {
        this.packages = packages;
        this.targets = targets;
        this.changeLog = changeLog;
        this.terminals = terminals;
        this.taxRates = taxRates;
        this.categories = categories;
        this.products = products;
        this.json = json;
        this.clock = clock;
    }

    // ---------- admin ----------

    /** Builds the zip and makes it available to the chosen tills ("Create & broadcast"). */
    public PackageView build(BuildRequest req, UUID adminUserId) {
        if (req.entities() == null || req.entities().isEmpty()) throw PosException.badRequest("Choose at least one entity type.");
        if (req.terminalIds() == null || req.terminalIds().isEmpty()) throw PosException.badRequest("Choose at least one till.");
        List<Terminal> tills = terminals.byIds(req.terminalIds()).stream().filter(Terminal::isActive).toList();
        if (tills.size() != req.terminalIds().size()) throw PosException.badRequest("Unknown or disabled till selected.");
        if (req.scope() == Scope.DATE_RANGE) {
            if (req.dateFrom() == null || req.dateTo() == null) throw PosException.badRequest("Choose the from and to dates.");
            if (req.dateFrom().isAfter(req.dateTo())) throw PosException.badRequest("The from date is after the to date.");
        }

        PackageContent content = collect(req);
        if (content.size() == 0) throw PosException.badRequest("Nothing to send: no changes for the chosen entities and dates.");

        int no = packages.nextPackageNo();
        Instant now = clock.instant();
        List<Entity> entities = req.entities().stream().sorted().toList();
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put(Entity.TAX_RATE.name(), content.taxRates().size());
        counts.put(Entity.CATEGORY.name(), content.categories().size());
        counts.put(Entity.PRODUCT.name(), content.products().size());
        Manifest manifest = new Manifest(Contract.CONTRACT_VERSION, no, now, req.scope().name(), req.dateFrom(), req.dateTo(),
            entities, counts, Map.of(), blankToNull(req.note()));
        byte[] zip = PackageZip.write(json, manifest, content);

        List<String> warnings = req.scope() == Scope.DATE_RANGE ? gapWarnings(tills, req.dateFrom()) : List.of();
        packages.save(new ConfigPackage(no, now, adminUserId, blankToNull(req.note()), req.scope().name(), req.dateFrom(),
            req.dateTo(), entities.stream().map(Enum::name).collect(Collectors.joining(",")), json.writeValueAsString(counts),
            PackageZip.sha256(zip), zip));
        for (Terminal t : tills) targets.save(new PackageTarget(no, t.getId(), now));
        return view(packages.findById(no).orElseThrow(), warnings);
    }

    @Transactional(readOnly = true)
    public List<PackageView> recent() {
        return packages.findTop50ByOrderByPackageNoDesc().stream().map(p -> view(p, List.of())).toList();
    }

    @Transactional(readOnly = true)
    public ConfigPackage get(int packageNo) {
        return packages.findById(packageNo).orElseThrow(() -> PosException.notFound("Package not found."));
    }

    /** [packageNo, createdAt] of the newest package a till applied, if any. */
    @Transactional(readOnly = true)
    public List<Object[]> lastApplied(UUID terminalId) {
        return targets.lastApplied(terminalId);
    }

    // ---------- tills ----------

    @Transactional(readOnly = true)
    public List<PackageInfo> waitingFor(Terminal till) {
        return targets.waitingFor(till.getId()).stream().map(t -> {
            ConfigPackage p = get(t.getPackageNo());
            return new PackageInfo(p.getPackageNo(), p.getCreatedAt(), p.getScope(), p.getDateFrom(), p.getDateTo(),
                entities(p), counts(p), p.getSha256(), p.getSizeBytes(), p.getNote());
        }).toList();
    }

    public byte[] download(Terminal till, int packageNo) {
        PackageTarget t = targets.findById(new PackageTarget.Key(packageNo, till.getId()))
            .orElseThrow(() -> PosException.notFound("Package " + packageNo + " is not for this till."));
        if ("PENDING".equals(t.getStatus())) t.mark("DOWNLOADED", null, clock.instant());
        return get(packageNo).getZip();
    }

    public void acknowledge(Terminal till, int packageNo, Contract.PackageAck ack) {
        PackageTarget t = targets.findById(new PackageTarget.Key(packageNo, till.getId()))
            .orElseThrow(() -> PosException.notFound("Package " + packageNo + " is not for this till."));
        String status = "APPLIED".equals(ack.status()) ? "APPLIED" : "FAILED";
        String error = status.equals("FAILED") ? truncate(ack.error() == null ? "Failed on the till." : ack.error()) : null;
        t.mark(status, error, clock.instant());
    }

    // ---------- helpers ----------

    /** Selected records (latest version) + everything they reference, so a till never gets a dangling reference. */
    private PackageContent collect(BuildRequest req) {
        Set<UUID> taxIds = new LinkedHashSet<>(), catIds = new LinkedHashSet<>(), productIds = new LinkedHashSet<>();
        boolean full = req.scope() == Scope.FULL;
        ZoneId zone = ZoneId.systemDefault();
        Instant from = full ? null : req.dateFrom().atStartOfDay(zone).toInstant();
        Instant to = full ? null : req.dateTo().plusDays(1).atStartOfDay(zone).toInstant();
        if (req.entities().contains(Entity.TAX_RATE))
            taxIds.addAll(full ? taxRates.findAll().stream().map(TaxRate::getId).toList() : changeLog.changedIds(Entity.TAX_RATE, from, to));
        if (req.entities().contains(Entity.CATEGORY))
            catIds.addAll(full ? categories.findAll().stream().map(Category::getId).toList() : changeLog.changedIds(Entity.CATEGORY, from, to));
        if (req.entities().contains(Entity.PRODUCT))
            productIds.addAll(full ? products.findAll().stream().map(Product::getId).toList() : changeLog.changedIds(Entity.PRODUCT, from, to));

        List<Product> prods = products.findAllById(productIds);
        for (Product p : prods) {                                   // auto-include references
            catIds.add(p.getCategory().getId());
            if (p.getTaxRate() != null) taxIds.add(p.getTaxRate().getId());
        }
        return new PackageContent(
            taxRates.findAllById(taxIds).stream().sorted(Comparator.comparing(TaxRate::getName)).map(CatalogMapper::toV1).toList(),
            categories.findAllById(catIds).stream().sorted(Comparator.comparing(Category::getName)).map(CatalogMapper::toV1).toList(),
            prods.stream().sorted(Comparator.comparing(Product::getName)).map(CatalogMapper::toV1).toList());
    }

    private List<String> gapWarnings(List<Terminal> tills, LocalDate from) {
        List<String> warnings = new ArrayList<>();
        Instant start = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
        for (Terminal t : tills) {
            var last = targets.lastApplied(t.getId());
            if (last.isEmpty()) {
                warnings.add(t.getName() + " has never applied a package; send a full snapshot so it has everything.");
            } else {
                Instant appliedPackageAt = ((java.sql.Timestamp) last.getFirst()[1]).toInstant();
                if (appliedPackageAt.isBefore(start)) {
                    warnings.add(t.getName() + " last applied package #" + last.getFirst()[0] + " from before " + from
                        + "; changes in between are not in this package.");
                }
            }
        }
        return warnings;
    }

    private PackageView view(ConfigPackage p, List<String> warnings) {
        Map<UUID, String> names = new java.util.HashMap<>();
        terminals.list().forEach(t -> names.put(t.getId(), t.getName()));
        List<TargetView> tv = targets.findByPackageNoOrderByTerminalId(p.getPackageNo()).stream()
            .map(t -> new TargetView(t.getTerminalId(), names.getOrDefault(t.getTerminalId(), "?"), t.getStatus(), t.getError(), t.getUpdatedAt()))
            .toList();
        return new PackageView(p.getPackageNo(), p.getCreatedAt(), p.getScope(), p.getDateFrom(), p.getDateTo(), entities(p),
            counts(p), p.getSizeBytes(), p.getSha256(), p.getNote(), tv, warnings);
    }

    private static List<Entity> entities(ConfigPackage p) {
        return Arrays.stream(p.getEntities().split(",")).map(Entity::valueOf).toList();
    }

    private Map<String, Integer> counts(ConfigPackage p) {
        return json.readValue(p.getItemCounts(), new TypeReference<Map<String, Integer>>() {});
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String truncate(String s) {
        return s.length() > 500 ? s.substring(0, 500) : s;
    }

    static Set<Entity> all() {
        return EnumSet.allOf(Entity.class);
    }
}
