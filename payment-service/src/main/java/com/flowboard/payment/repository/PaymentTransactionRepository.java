package com.flowboard.payment.repository;

import com.flowboard.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
    PaymentTransaction findByRazorpayOrderId(String razorpayOrderId);
}
