package com.newproject.notification.controller;

import com.newproject.notification.dto.OrderConfirmationRequest;
import com.newproject.notification.service.OrderConfirmationEmailService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final OrderConfirmationEmailService orderConfirmationEmailService;

    public NotificationController(OrderConfirmationEmailService orderConfirmationEmailService) {
        this.orderConfirmationEmailService = orderConfirmationEmailService;
    }

    @GetMapping("/ping")
    public String ping() {
        return "ok";
    }

    @PostMapping("/order-confirmation")
    public ResponseEntity<Map<String, Object>> sendOrderConfirmation(@RequestBody OrderConfirmationRequest request) {
        boolean sent = orderConfirmationEmailService.sendOrderConfirmation(request);
        return ResponseEntity.accepted().body(Map.of("accepted", true, "sent", sent));
    }
}
