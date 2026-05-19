package com.flowboard.payment.controller;

import com.flowboard.payment.dto.PaymentOrderRequest;
import com.flowboard.payment.dto.PaymentVerifyRequest;
import com.flowboard.payment.entity.PaymentTransaction;
import com.flowboard.payment.entity.Subscription;
import com.flowboard.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            @RequestBody PaymentOrderRequest request,
            @RequestHeader(value = "X-user-id", required = false) Long userId) {
        try {
            if (request.getUserId() == null) {
                request.setUserId(userId);
            }
            PaymentTransaction txn = paymentService.createOrder(request);
            return ResponseEntity.ok(Map.of(
                    "orderId", txn.getRazorpayOrderId(),
                    "amount", txn.getAmount(),
                    "currency", txn.getCurrency()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody PaymentVerifyRequest request) {
        try {
            PaymentTransaction txn = paymentService.verifyPayment(request);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Payment verified and subscription upgraded"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/workspace/{workspaceId}/subscription")
    public ResponseEntity<Subscription> getSubscription(@PathVariable Long workspaceId) {
        return ResponseEntity.ok(paymentService.getSubscription(workspaceId));
    }

    @GetMapping("/workspace/{workspaceId}/history")
    public ResponseEntity<List<PaymentTransaction>> getTransactionHistory(@PathVariable Long workspaceId) {
        return ResponseEntity.ok(paymentService.getTransactionHistory(workspaceId));
    }
}
