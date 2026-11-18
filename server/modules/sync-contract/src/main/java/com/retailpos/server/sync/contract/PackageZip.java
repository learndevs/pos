package com.retailpos.server.sync.contract;

import com.retailpos.server.sync.contract.Contract.CategoryV1;
import com.retailpos.server.sync.contract.Contract.Manifest;
import com.retailpos.server.sync.contract.Contract.PackageContent;
import com.retailpos.server.sync.contract.Contract.ProductV1;
import com.retailpos.server.sync.contract.Contract.TaxRateV1;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * The package zip format, written by admin and read by the till with this same code:
 *   manifest.json, tax_rates.json, categories.json, products.json (JSON arrays of full records).
 * The manifest holds the SHA-256 of every data file; reading verifies them and the contract version.
 */
public final class PackageZip {

    public static final String MANIFEST = "manifest.json";
    public static final String TAX_RATES = "tax_rates.json";
    public static final String CATEGORIES = "categories.json";
    public static final String PRODUCTS = "products.json";
    public static final String MENU_FOLDERS = "menu_folders.json";
    public static final String MENU_BUTTONS = "menu_buttons.json";
    private static final long MAX_ENTRY_BYTES = 50L * 1024 * 1024;   // guard against zip bombs

    public record Read(Manifest manifest, PackageContent content) {}

    private PackageZip() {}

    /** Builds the zip. The manifest's sha256 map is filled in here. */
    public static byte[] write(ObjectMapper json, Manifest manifest, PackageContent content) {
        Map<String, byte[]> files = new LinkedHashMap<>();
        files.put(TAX_RATES, json.writeValueAsBytes(content.taxRates()));
        files.put(CATEGORIES, json.writeValueAsBytes(content.categories()));
        files.put(PRODUCTS, json.writeValueAsBytes(content.products()));
        files.put(MENU_FOLDERS, json.writeValueAsBytes(content.menuFolders()));
        files.put(MENU_BUTTONS, json.writeValueAsBytes(content.menuButtons()));
        Map<String, String> sums = new LinkedHashMap<>();
        files.forEach((name, bytes) -> sums.put(name, sha256(bytes)));
        Manifest m = new Manifest(manifest.contractVersion(), manifest.packageNo(), manifest.createdAt(), manifest.scope(),
            manifest.dateFrom(), manifest.dateTo(), manifest.entities(), manifest.counts(), sums, manifest.note());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            put(zip, MANIFEST, json.writerWithDefaultPrettyPrinter().writeValueAsBytes(m));
            for (var f : files.entrySet()) put(zip, f.getKey(), f.getValue());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    /** Reads and verifies a package zip. Throws IllegalStateException with a readable reason if it is not valid. */
    public static Read read(ObjectMapper json, byte[] zipBytes) {
        Map<String, byte[]> files = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8)) {
            for (ZipEntry e; (e = zip.getNextEntry()) != null; ) {
                if (e.isDirectory() || e.getName().contains("..") || e.getName().contains("/")) continue;
                byte[] data = zip.readNBytes((int) Math.min(MAX_ENTRY_BYTES, Integer.MAX_VALUE - 8));
                if (data.length >= MAX_ENTRY_BYTES) throw new IllegalStateException("Package file too large: " + e.getName());
                files.put(e.getName(), data);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Package is not a valid zip: " + e.getMessage());
        }
        byte[] manifestBytes = files.get(MANIFEST);
        if (manifestBytes == null) throw new IllegalStateException("Package has no manifest.json.");
        Manifest m = json.readValue(manifestBytes, Manifest.class);
        if (m.contractVersion() != Contract.CONTRACT_VERSION) {
            throw new IllegalStateException("Package uses sync contract v" + m.contractVersion()
                + ", this till understands v" + Contract.CONTRACT_VERSION + ". Update the POS.");
        }
        for (var sum : m.sha256().entrySet()) {
            byte[] data = files.get(sum.getKey());
            if (data == null) throw new IllegalStateException("Package is missing " + sum.getKey() + ".");
            if (!sha256(data).equals(sum.getValue())) throw new IllegalStateException("Checksum mismatch in " + sum.getKey() + ".");
        }
        PackageContent content = new PackageContent(
            list(json, files.get(TAX_RATES), new TypeReference<List<TaxRateV1>>() {}),
            list(json, files.get(CATEGORIES), new TypeReference<List<CategoryV1>>() {}),
            list(json, files.get(PRODUCTS), new TypeReference<List<ProductV1>>() {}),
            list(json, files.get(MENU_FOLDERS), new TypeReference<List<Contract.MenuFolderV1>>() {}),     // absent in older packages = empty
            list(json, files.get(MENU_BUTTONS), new TypeReference<List<Contract.MenuButtonV1>>() {}));
        return new Read(m, content);
    }

    private static <T> List<T> list(ObjectMapper json, byte[] data, TypeReference<List<T>> type) {
        return data == null ? List.of() : json.readValue(data, type);
    }

    private static void put(ZipOutputStream zip, String name, byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }

    public static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
