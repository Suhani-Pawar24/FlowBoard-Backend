package com.flowboard.payment.service;

import com.flowboard.payment.dto.PaymentOrderRequest;
import com.flowboard.payment.dto.PaymentVerifyRequest;
import com.flowboard.payment.entity.PaymentTransaction;
import com.flowboard.payment.entity.Subscription;

import java.util.List;

public interface PaymentService {
    PaymentTransaction createOrder(PaymentOrderRequest request);
    PaymentTransaction verifyPayment(PaymentVerifyRequest request);
    Subscription getSubscription(Long workspaceId);
    List<PaymentTransaction> getTransactionHistory(Long workspaceId);
}
