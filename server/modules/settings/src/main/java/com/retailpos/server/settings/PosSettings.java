package com.retailpos.server.settings;

import java.util.Map;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Typed access to app_setting values used by business rules. */
@Service
@Transactional(readOnly = true)
public class PosSettings {

    private final AppSettingRepository settings;

    public PosSettings(AppSettingRepository settings) {
        this.settings = settings;
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
