package com.restaurant.model;

import java.math.BigDecimal;

public record RevenueSummary(
        long invoiceCount,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal totalAmount
) {
}