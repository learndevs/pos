package com.retailpos.server.sync.client;

import com.retailpos.server.common.PosException;
import com.retailpos.server.settings.PosSettings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This till's identity and its connection to the admin app (independent tills: each connects with its own token).
 * The token is write-only: it is never returned.
 */
@RestController
@RequestMapping("/api/device")
public class DeviceController {

    public record DeviceView(String code, String name, Integer tillNo, String terminalId, boolean connected, String adminUrl, boolean fromConfig) {}
    public record ConnectRequest(@NotBlank @Size(max = 200) String adminUrl, @NotBlank @Size(max = 200) String token) {}

    private final PosSettings settings;
    private final SyncClient sync;

    DeviceController(PosSettings settings, SyncClient sync) {
        this.settings = settings;
        this.sync = sync;
    }

    @GetMapping
    public DeviceView device() {
        var d = settings.device();
        var db = settings.syncConnection();
        var st = sync.status();
        return new DeviceView(d.code(), d.name(), d.tillNo(), d.terminalId() == null ? null : d.terminalId().toString(), st.enabled(),
            st.adminUrl(), st.enabled() && !db.isSet());
    }

    /** Connect this till to admin: checks URL + token against admin, stores them, takes the code/name admin assigned. */
    @PutMapping("/connection")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public DeviceView connect(@Valid @RequestBody ConnectRequest req) {
        String url = req.adminUrl().trim().replaceAll("/+$", "");
        com.retailpos.server.sync.contract.Contract.TerminalInfo me;
        try {
            me = sync.whoAmI(url, req.token().trim());
        } catch (RuntimeException e) {
            throw PosException.badRequest("Could not connect to the admin app with that address and token.");
        }
        settings.saveSyncConnection(new PosSettings.SyncConnection(url, req.token().trim()));
        settings.saveDevice(new PosSettings.Device(me.code(), me.name(), me.terminalId(), me.tillNo()));
        sync.syncNow();
        return device();
    }

    /** Back to a stand-alone till (keeps its data and device code). */
    @DeleteMapping("/connection")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public DeviceView disconnect() {
        settings.saveSyncConnection(new PosSettings.SyncConnection(null, null));
        return device();
    }
}
