package com.flowboard.payment.entity;

public enum SubscriptionTier {
    FREE(0),
    PRO(1500),         // Monthly price in INR for example
    ENTERPRISE(5000);  // Monthly price in INR

    private final int monthlyPriceInr;

    SubscriptionTier(int monthlyPriceInr) {
        this.monthlyPriceInr = monthlyPriceInr;
    }

    public int getMonthlyPriceInr() {
        return monthlyPriceInr;
    }
}
