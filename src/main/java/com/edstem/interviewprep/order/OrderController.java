package com.edstem.interviewprep.order;

import com.edstem.interviewprep.common.error.BadRequestException;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    static final String REPLAYED_HEADER = "Idempotent-Replayed";
    private static final int MAX_KEY_LENGTH = 100;

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> place(@RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
                                               @Valid @RequestBody PlaceOrderRequest request,
                                               Authentication authentication) {
        String key = idempotencyKey.trim();
        if (key.isEmpty() || key.length() > MAX_KEY_LENGTH) {
            throw new BadRequestException(IDEMPOTENCY_KEY_HEADER,
                    "Idempotency-Key must be between 1 and " + MAX_KEY_LENGTH + " characters");
        }
        OrderService.PlacementResult result = orderService.place(authentication.getName(), key, request);
        if (result.created()) {
            return ResponseEntity.created(URI.create("/api/orders/" + result.order().id()))
                    .header(REPLAYED_HEADER, "false")
                    .body(result.order());
        }
        return ResponseEntity.status(HttpStatus.OK).header(REPLAYED_HEADER, "true").body(result.order());
    }

    @GetMapping
    public List<OrderResponse> listMine(Authentication authentication) {
        return orderService.listMine(authentication.getName());
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id, Authentication authentication) {
        return orderService.get(id, authentication.getName(), isAdmin(authentication));
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id, Authentication authentication) {
        return orderService.cancel(id, authentication.getName(), isAdmin(authentication));
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
