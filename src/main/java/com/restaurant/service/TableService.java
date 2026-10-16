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

    public List<RestaurantTable> search(
            String keyword,
            TableStatus status
    ) throws SQLException {

        return tableDAO.search(keyword, status);
    }

    public long addTable(
            String tableNumber,
            int capacity
    ) throws SQLException {

        String normalizedTableNumber =
                validateAndNormalizeTableNumber(
                        tableNumber
                );

        validateCapacity(capacity);

        if (tableDAO.existsByTableNumber(
                normalizedTableNumber,
                null
        )) {
            throw new IllegalArgumentException(
                    "Số bàn đã tồn tại"
            );
        }

        return tableDAO.insert(
                normalizedTableNumber,
                capacity
        );
    }

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

    private void validateCapacity(int capacity) {
        if (capacity <= 0 || capacity > 50) {
            throw new IllegalArgumentException(
                    "Sức chứa phải từ 1 đến 50 người"
            );
        }
    }
}