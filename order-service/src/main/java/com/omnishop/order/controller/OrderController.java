package com.omnishop.order.controller;

import com.omnishop.order.dto.*;
import com.omnishop.order.service.OrderAnalyticsService;
import com.omnishop.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order placement and tracking")
public class OrderController {

    private final OrderService orderService;
    private final OrderAnalyticsService analyticsService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place order",
            description = "Validates live price/stock against product-service, snapshots prices, "
                    + "computes the total with Streams. "
                    + "Example body: {\"userId\":1,\"items\":[{\"productId\":1,\"quantity\":2}]}")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order placed with status PENDING"),
        @ApiResponse(responseCode = "400", description = "Validation failed or insufficient stock"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "503", description = "Product service unavailable")
    })
    public OrderResponseDTO create(@Valid @RequestBody OrderRequestDTO request) {
        return orderService.createOrder(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by id", description = "Example: GET /api/v1/orders/1")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "404", description = "No order with that id")
    })
    public OrderResponseDTO getById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List orders for a user", description = "Example: GET /api/v1/orders/user/1")
    @ApiResponse(responseCode = "200", description = "User's order list")
    public List<OrderResponseDTO> getByUser(@PathVariable Long userId) {
        return orderService.getOrdersByUser(userId);
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel order", description = "Only PENDING orders can be cancelled.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order cancelled"),
        @ApiResponse(responseCode = "400", description = "Order is not PENDING"),
        @ApiResponse(responseCode = "404", description = "No order with that id")
    })
    public OrderResponseDTO cancel(@PathVariable Long id) {
        return orderService.cancelOrder(id);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update order status",
            description = "Admin operations: PROCESSING, SHIPPED, DELIVERED, etc. "
                    + "Example body: {\"status\":\"SHIPPED\"}. Role check arrives in Wave 2.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated"),
        @ApiResponse(responseCode = "400", description = "Invalid status value"),
        @ApiResponse(responseCode = "404", description = "No order with that id")
    })
    public OrderResponseDTO updateStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        return orderService.updateStatus(id, body.get("status"));
    }

    @GetMapping
    @Operation(summary = "List all orders",
            description = "Optional ?status= filter (e.g. /api/v1/orders?status=PENDING).")
    @ApiResponse(responseCode = "200", description = "Order list")
    public List<OrderResponseDTO> getAll(@RequestParam(required = false) String status) {
        return orderService.getAllOrders(status);
    }

    @GetMapping("/high-value")
    @Operation(summary = "High-value orders",
            description = "Orders above ?min= total, most expensive first. Example: /api/v1/orders/high-value?min=5000")
    @ApiResponse(responseCode = "200", description = "Filtered order list")
    public List<OrderResponseDTO> highValue(@RequestParam(defaultValue = "5000") BigDecimal min) {
        return orderService.getHighValueOrders(min);
    }

    @GetMapping("/analytics/summary")
    @Operation(summary = "Admin analytics summary",
            description = "Total orders, revenue total, revenue-by-day (30d) and status "
                    + "distribution, computed with Streams groupingBy. ADMIN role required.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Analytics map"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Admin role required")
    })
    public java.util.Map<String, Object> analytics() {
        return analyticsService.summary();
    }
}
