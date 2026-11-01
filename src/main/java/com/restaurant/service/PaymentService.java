package com.restaurant.service;

import com.restaurant.dao.PaymentDAO;
import com.restaurant.model.PayableOrder;
import com.restaurant.model.PaymentMethod;
import com.restaurant.model.PaymentReceipt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;

public class PaymentService {

    private final PaymentDAO paymentDAO;

    public PaymentService(PaymentDAO paymentDAO) {
        if (paymentDAO == null) {
            throw new IllegalArgumentException(
                    "PaymentDAO không được để trống"
            );
        }

        this.paymentDAO = paymentDAO;
    }

    public List<PayableOrder> getPayableOrders()
            throws SQLException {

        return paymentDAO.findPayableOrders();
    }

    public PaymentReceipt pay(
            long orderId,
            long cashierId,
            BigDecimal discountAmount,
            PaymentMethod paymentMethod
    ) throws SQLException {

        if (orderId <= 0) {
            throw new IllegalArgumentException(
                    "Mã đơn gọi món không hợp lệ"
            );
        }

        if (cashierId <= 0) {
            throw new IllegalArgumentException(
                    "Mã nhân viên thanh toán không hợp lệ"
            );
        }

        if (paymentMethod == null) {
            throw new IllegalArgumentException(
                    "Vui lòng chọn phương thức thanh toán"
            );
        }

        BigDecimal normalizedDiscount =
                normalizeDiscount(discountAmount);

        return paymentDAO.pay(
                orderId,
                cashierId,
                normalizedDiscount,
                paymentMethod
        );
    }

    private BigDecimal normalizeDiscount(
            BigDecimal discountAmount
    ) {
        if (discountAmount == null) {
            return BigDecimal.ZERO.setScale(2);
        }

        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Tiền giảm giá không được âm"
            );
        }

        try {
            return discountAmount.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Tiền giảm giá chỉ được có tối đa 2 chữ số thập phân"
            );
        }
    }
}