package com.flowboard.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long workspaceId;

    @Column(nullable = true)
    private Long userId;

    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;

    private int amount;
    private String currency;

    @Enumerated(EnumType.STRING)
    private SubscriptionTier targetTier;

    private String status; // CREATED, SUCCESS, FAILED

    private LocalDateTime createdAt = LocalDateTime.now();
}
