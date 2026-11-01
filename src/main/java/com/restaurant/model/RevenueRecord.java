package com.restaurant.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RevenueRecord(
        long paymentId,
        String invoiceCode,
        long orderId,
        String tableNumber,
        String cashierName,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        LocalDateTime paidAt
) {
}