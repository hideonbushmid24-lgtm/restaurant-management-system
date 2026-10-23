package com.restaurant.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentReceipt {

    private final long paymentId;
    private final String invoiceCode;
    private final long orderId;
    private final String tableNumber;
    private final BigDecimal subtotal;
    private final BigDecimal discountAmount;
    private final BigDecimal totalAmount;
    private final PaymentMethod paymentMethod;
    private final String cashierName;
    private final LocalDateTime paidAt;

    public PaymentReceipt(
            long paymentId,
            String invoiceCode,
            long orderId,
            String tableNumber,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            PaymentMethod paymentMethod,
            String cashierName,
            LocalDateTime paidAt
    ) {
        this.paymentId = paymentId;
        this.invoiceCode = invoiceCode;
        this.orderId = orderId;
        this.tableNumber = tableNumber;
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.cashierName = cashierName;
        this.paidAt = paidAt;
    }

    public long getPaymentId() {
        return paymentId;
    }

    public String getInvoiceCode() {
        return invoiceCode;
    }

    public long getOrderId() {
        return orderId;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getCashierName() {
        return cashierName;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }
}