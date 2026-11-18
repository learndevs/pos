package com.retailpos.server.sales;

import com.retailpos.server.identity.SessionUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales")
@PreAuthorize("hasAuthority('ORDER_CREATE')")
public class SalesController {

    public record AddItem(@NotNull UUID productId, BigDecimal quantity) {}
    public record Quantity(@NotNull BigDecimal quantity) {}
    public record Tender(@NotNull @Pattern(regexp = "CASH|CARD|OTHER") String method, @Positive Long amountMinor) {}
    public record OrderType(@NotNull @Pattern(regexp = "DINE_IN|TAKEAWAY") String orderType) {}
    public record Discount(@NotNull @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(100) Integer percent) {}
    public record Cancel(@NotBlank @Size(max = 200) String reason) {}

    private final SalesService sales;

    public SalesController(SalesService sales) {
        this.sales = sales;
    }

    /** The signed-in user's OPEN sale, or 204 when there is none. */
    @GetMapping("/current")
    public ResponseEntity<SaleView> current(@AuthenticationPrincipal SessionUser user) {
        return sales.currentFor(user.id()).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Returns the user's OPEN sale or starts a new one. */
    @PostMapping
    public SaleView start(@AuthenticationPrincipal SessionUser user) {
        return sales.start(user.id());
    }

    @PostMapping("/{id}/items")
    public SaleView addItem(@PathVariable UUID id, @Valid @RequestBody AddItem req) {
        return sales.addItem(id, req.productId(), req.quantity() == null ? BigDecimal.ONE : req.quantity());
    }

    @PutMapping("/{id}/items/{itemId}/quantity")
    public SaleView quantity(@PathVariable UUID id, @PathVariable UUID itemId, @Valid @RequestBody Quantity req) {
        return sales.setQuantity(id, itemId, req.quantity());
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public SaleView removeLine(@PathVariable UUID id, @PathVariable UUID itemId) {
        return sales.removeLine(id, itemId);
    }

    @PostMapping("/{id}/items/{itemId}/discount")
    public SaleView discount(@PathVariable UUID id, @PathVariable UUID itemId, @Valid @RequestBody Discount req,
                             @AuthenticationPrincipal SessionUser user) {
        return sales.discountLine(id, itemId, req.percent(), user);
    }

    @PutMapping("/{id}/order-type")
    public SaleView orderType(@PathVariable UUID id, @Valid @RequestBody OrderType req) {
        return sales.setOrderType(id, req.orderType());
    }

    @PostMapping("/{id}/tenders")
    public SaleView tender(@PathVariable UUID id, @Valid @RequestBody Tender req) {
        return sales.tender(id, req.method(), req.amountMinor());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('ORDER_VOID')")
    public SaleView cancel(@PathVariable UUID id, @Valid @RequestBody Cancel req, @AuthenticationPrincipal SessionUser user) {
        return sales.cancel(id, user.id(), req.reason());
    }
}
