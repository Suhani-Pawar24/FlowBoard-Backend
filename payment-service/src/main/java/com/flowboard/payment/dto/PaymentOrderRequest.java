package com.flowboard.payment.dto;

import com.flowboard.payment.entity.SubscriptionTier;
import lombok.Data;

@Data
public class PaymentOrderRequest {
    private Long workspaceId;
    private Long userId;
    private SubscriptionTier targetTier;
}
