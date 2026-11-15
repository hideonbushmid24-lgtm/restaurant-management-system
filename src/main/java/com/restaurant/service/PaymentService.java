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

    /**
     * Số nguyên VND, có thể kèm dấu phân cách hàng nghìn (. hoặc ,):
     * 50000, 50.000, 1,250,000. Dấu "-" ở đầu được nhận để thông báo
     * "không được âm" ở bước kiểm tra sau.
     */
    private static final String DISCOUNT_PATTERN =
            "-?(\\d+|\\d{1,3}([.,]\\d{3})+)";

    private final PaymentDAO paymentDAO;

    public PaymentService(PaymentDAO paymentDAO) {
        if (paymentDAO == null) {
            throw new IllegalArgumentException(
                    "PaymentDAO không được để trống"
            );
        }

        this.paymentDAO = paymentDAO;
    }

    /**
     * Đọc số tiền giảm giá do người dùng nhập. Để trống nghĩa là 0.
     *
     * Không còn xóa mọi dấu "." và ",": cách đó biến "1,5" thành 15 và
     * "50.5" thành 505 mà không báo lỗi. VND không có phần thập phân nên
     * chỉ nhận số nguyên, có thể có dấu phân cách hàng nghìn.
     *
     * @throws IllegalArgumentException nếu không phải số nguyên VND hợp lệ
     */
    public static BigDecimal parseDiscountInput(String text) {
        if (text == null || text.isBlank()) {
            return BigDecimal.ZERO;
        }

        String trimmed = text.trim().replace(" ", "");

        if (!trimmed.matches(DISCOUNT_PATTERN)) {
            throw new IllegalArgumentException(
                    "Tiền giảm giá phải là số nguyên (VND), "
                            + "ví dụ 50000 hoặc 50.000"
            );
        }

        return new BigDecimal(
                trimmed.replace(".", "").replace(",", "")
        );
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