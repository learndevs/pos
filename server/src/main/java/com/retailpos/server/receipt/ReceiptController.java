package com.retailpos.server.receipt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReceiptController {

    public record TemplateDto(UUID id, String name, String html, int paperWidthMm) {
        static TemplateDto of(ReceiptTemplate t) {
            return new TemplateDto(t.getId(), t.getName(), t.getHtml(), t.getPaperWidthMm());
        }
    }

    public record TemplateUpdate(@NotBlank @Size(max = 80) String name, @NotBlank @Size(max = 200_000) String html,
                                 @Min(40) @Max(120) int paperWidthMm) {}

    public record PreviewRequest(@NotBlank @Size(max = 200_000) String html, UUID transactionId) {}

    private final ReceiptService receipts;

    public ReceiptController(ReceiptService receipts) {
        this.receipts = receipts;
    }

    /** Rendered receipt HTML for a completed sale (the desktop prints it). */
    @GetMapping(value = "/api/receipts/{txnId}", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasAuthority('ORDER_CREATE')")
    public String receipt(@PathVariable UUID txnId) {
        return receipts.render(txnId);
    }

    @GetMapping("/api/settings/receipt-template")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public TemplateDto template() {
        return TemplateDto.of(receipts.activeTemplate());
    }

    @PutMapping("/api/settings/receipt-template")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public TemplateDto update(@Valid @RequestBody TemplateUpdate req) {
        return TemplateDto.of(receipts.updateActive(req.name(), req.html(), req.paperWidthMm()));
    }

    /** Renders unsaved template HTML against a sale (or sample data) so it can be checked before saving. */
    @PostMapping(value = "/api/settings/receipt-template/preview", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public String preview(@Valid @RequestBody PreviewRequest req) {
        return receipts.preview(req.html(), req.transactionId());
    }
}
