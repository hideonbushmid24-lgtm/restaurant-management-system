package com.restaurant.service;

import com.restaurant.dao.TableDAO;
import com.restaurant.model.RestaurantTable;
import com.restaurant.model.TableStatus;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public class TableService {

    private final TableDAO tableDAO;

    public TableService(TableDAO tableDAO) {
        if (tableDAO == null) {
            throw new IllegalArgumentException(
                    "TableDAO không được null"
            );
        }

        this.tableDAO = tableDAO;
    }

    /**
     * Tìm kiếm và lọc danh sách bàn.
     */
    public List<RestaurantTable> search(
            String keyword,
            TableStatus status
    ) throws SQLException {

        return tableDAO.search(keyword, status);
    }

    /**
     * Thêm một bàn mới.
     *
     * Nếu số bàn đã từng bị xóa mềm thì kích hoạt lại
     * bản ghi cũ thay vì tạo bản ghi trùng.
     */
    public long addTable(
            String tableNumber,
            int capacity
    ) throws SQLException {

        String normalizedTableNumber =
                validateAndNormalizeTableNumber(
                        tableNumber
                );

        validateCapacity(capacity);

        /*
         * Nếu bàn đang hoạt động thì không cho phép
         * thêm một bàn có cùng số bàn.
         */
        if (tableDAO.existsByTableNumber(
                normalizedTableNumber,
                null
        )) {
            throw new IllegalArgumentException(
                    "Số bàn đã tồn tại"
            );
        }

        /*
         * Kiểm tra bàn có từng bị xóa mềm hay không.
         */
        Long inactiveTableId =
                tableDAO.findInactiveIdByTableNumber(
                        normalizedTableNumber
                );

        /*
         * Nếu bàn đã bị xóa mềm thì kích hoạt lại.
         */
        if (inactiveTableId != null) {
            boolean reactivated =
                    tableDAO.reactivate(
                            inactiveTableId,
                            capacity
                    );

            if (!reactivated) {
                throw new IllegalArgumentException(
                        "Không thể kích hoạt lại bàn"
                );
            }

            return inactiveTableId;
        }

        /*
         * Nếu số bàn chưa từng tồn tại thì tạo mới.
         */
        return tableDAO.insert(
                normalizedTableNumber,
                capacity
        );
    }

    /**
     * Cập nhật thông tin và trạng thái bàn.
     */
    public void updateTable(
            long id,
            String tableNumber,
            int capacity,
            TableStatus status
    ) throws SQLException {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Mã bàn không hợp lệ"
            );
        }

        if (status == null
                || status == TableStatus.INACTIVE) {

            throw new IllegalArgumentException(
                    "Trạng thái bàn không hợp lệ"
            );
        }

        String normalizedTableNumber =
                validateAndNormalizeTableNumber(
                        tableNumber
                );

        validateCapacity(capacity);

        /*
         * Kiểm tra một bàn đang hoạt động khác có
         * sử dụng số bàn này hay chưa.
         */
        if (tableDAO.existsByTableNumber(
                normalizedTableNumber,
                id
        )) {
            throw new IllegalArgumentException(
                    "Số bàn đã tồn tại"
            );
        }

        boolean updated = tableDAO.update(
                id,
                normalizedTableNumber,
                capacity,
                status
        );

        if (!updated) {
            throw new IllegalArgumentException(
                    "Không tìm thấy bàn cần cập nhật"
            );
        }
    }

    /**
     * Xóa mềm một bàn đang trống.
     */
    public void deleteTable(long id)
            throws SQLException {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Mã bàn không hợp lệ"
            );
        }

        boolean deleted = tableDAO.deactivate(id);

        if (!deleted) {
            throw new IllegalArgumentException(
                    "Chỉ có thể xóa bàn đang ở trạng thái trống"
            );
        }
    }

    /**
     * Kiểm tra và chuẩn hóa số bàn.
     */
    private String validateAndNormalizeTableNumber(
            String tableNumber
    ) {
        if (tableNumber == null
                || tableNumber.isBlank()) {

            throw new IllegalArgumentException(
                    "Số bàn không được để trống"
            );
        }

        String normalized =
                tableNumber
                        .trim()
                        .toUpperCase(Locale.ROOT);

        if (normalized.length() > 20) {
            throw new IllegalArgumentException(
                    "Số bàn không được vượt quá 20 ký tự"
            );
        }

        if (!normalized.matches("[A-Z0-9-]+")) {
            throw new IllegalArgumentException(
                    "Số bàn chỉ được chứa chữ, số và dấu gạch ngang"
            );
        }

        return normalized;
    }

    /**
     * Kiểm tra sức chứa của bàn.
     */
    private void validateCapacity(int capacity) {
        if (capacity <= 0 || capacity > 50) {
            throw new IllegalArgumentException(
                    "Sức chứa phải từ 1 đến 50 người"
            );
        }
    }
}