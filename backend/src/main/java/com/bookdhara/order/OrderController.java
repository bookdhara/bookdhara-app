package com.bookdhara.order;

import com.bookdhara.order.Order.DeliveryMethod;
import com.bookdhara.order.OrderDtos.CancelRequest;
import com.bookdhara.order.OrderDtos.HandoverRequest;
import com.bookdhara.order.OrderDtos.OrderView;
import com.bookdhara.order.OrderDtos.PlaceOrderRequest;
import com.bookdhara.order.OrderDtos.Quote;
import com.bookdhara.order.OrderDtos.ShipRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping("/quote")
    public Quote quote(@AuthenticationPrincipal Long userId, @RequestParam Long bookId,
                       @RequestParam DeliveryMethod method, @RequestParam(defaultValue = "0") int credits) {
        return service.quote(userId, bookId, method, credits);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView place(@AuthenticationPrincipal Long userId, @Valid @RequestBody PlaceOrderRequest request) {
        return service.place(userId, request);
    }

    /** role=buying (default) or role=selling. */
    @GetMapping
    public List<OrderView> mine(@AuthenticationPrincipal Long userId, @RequestParam(defaultValue = "buying") String role) {
        return service.mine(userId, role);
    }

    @GetMapping("/{id}")
    public OrderView get(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return service.get(id, userId);
    }

    @PostMapping("/{id}/ship")
    public OrderView ship(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                          @Valid @RequestBody(required = false) ShipRequest request) {
        return service.ship(id, userId, request == null ? null : request.trackingNote());
    }

    @PostMapping("/{id}/confirm-received")
    public OrderView confirmReceived(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return service.confirmReceived(id, userId);
    }

    @PostMapping("/{id}/handover")
    public OrderView handover(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                              @Valid @RequestBody HandoverRequest request) {
        return service.handover(id, userId, request.code());
    }

    @PostMapping("/{id}/cancel")
    public OrderView cancel(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                            @Valid @RequestBody(required = false) CancelRequest request) {
        return service.cancel(id, userId, request == null ? null : request.reason());
    }
}
