package com.retailpos.server.settings;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class SettingsController {

    private final AppSettingRepository settings;

    public SettingsController(AppSettingRepository settings) {
        this.settings = settings;
    }

    /** Business name, currency etc. for the header and money formatting. */
    @GetMapping(value = "/api/settings/business", produces = MediaType.APPLICATION_JSON_VALUE)
    public String business() {
        return settings.findById("business").map(AppSetting::getValue)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business settings missing."));
    }
}
