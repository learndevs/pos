package com.retailpos.server.settings;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Typed access to app_setting values used by business rules. */
@Service
@Transactional(readOnly = true)
public class PosSettings {

    /** This till's identity. code is printed in receipt numbers (T01-...); terminalId is set once connected to admin. */
    public record Device(String code, String name, UUID terminalId, Integer tillNo) {
        public Device(String code, String name, UUID terminalId) { this(code, name, terminalId, null); }
    }

    /** Admin connection set on the till (Device panel); null fields = not connected (stand-alone). */
    public record SyncConnection(String adminUrl, String token) {
        public boolean isSet() { return adminUrl != null && !adminUrl.isBlank() && token != null && !token.isBlank(); }
    }

    private final AppSettingRepository settings;
    private final Clock clock;
    private final tools.jackson.databind.ObjectMapper json;

    public PosSettings(AppSettingRepository settings, Clock clock, tools.jackson.databind.ObjectMapper json) {
        this.settings = settings;
        this.clock = clock;
        this.json = json;
    }

    public Device device() {
        Map<String, Object> d = json("device");
        Object id = d.get("terminalId");
        Object no = d.get("tillNo");
        return new Device(String.valueOf(d.getOrDefault("code", "T01")), (String) d.get("name"),
            id == null ? null : UUID.fromString(String.valueOf(id)), no instanceof Number n ? n.intValue() : null);
    }

    @Transactional
    public void saveDevice(Device d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", d.code());
        m.put("name", d.name());
        m.put("terminalId", d.terminalId() == null ? null : d.terminalId().toString());
        m.put("tillNo", d.tillNo());
        save("device", m);
    }

    public SyncConnection syncConnection() {
        Map<String, Object> s = json("sync");
        return new SyncConnection((String) s.get("adminUrl"), (String) s.get("token"));
    }

    @Transactional
    public void saveSyncConnection(SyncConnection c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("adminUrl", c.adminUrl());
        m.put("token", c.token());
        save("sync", m);
    }

    private void save(String key, Map<String, Object> value) {
        String v = json.writeValueAsString(value);
        settings.findById(key).ifPresentOrElse(s -> s.setValue(v, clock.instant()),
            () -> settings.save(new AppSetting(key, v, clock.instant())));
    }


    /** Highest line discount % a user without DISCOUNT_UNLIMITED may give (app_setting "discount"). Default 10. */
    public int cashierMaxDiscountPercent() {
        return json("discount").get("cashierMaxPercent") instanceof Number n ? n.intValue() : 10;
    }

    private Map<String, Object> json(String key) {
        return settings.findById(key).map(AppSetting::getValue)
            .map(v -> JsonParserFactory.getJsonParser().parseMap(v)).orElse(Map.of());
    }
}
