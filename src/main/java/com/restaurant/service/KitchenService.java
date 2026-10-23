package com.restaurant.service;

import com.restaurant.dao.KitchenDAO;
import com.restaurant.model.KitchenItem;
import com.restaurant.model.KitchenStatus;

import java.sql.SQLException;
import java.util.List;

public class KitchenService {

    private final KitchenDAO kitchenDAO;

    public KitchenService(
            KitchenDAO kitchenDAO
    ) {
        if (kitchenDAO == null) {
            throw new IllegalArgumentException(
                    "KitchenDAO không được null"
            );
        }

        this.kitchenDAO = kitchenDAO;
    }

    public List<KitchenItem> getActiveItems()
            throws SQLException {

        return kitchenDAO.findActiveItems();
    }

    public void startPreparing(
            long orderItemId
    ) throws SQLException {

        validateOrderItemId(orderItemId);

        kitchenDAO.updateStatus(
                orderItemId,
                KitchenStatus.PREPARING
        );
    }

    public void markReady(
            long orderItemId
    ) throws SQLException {

        validateOrderItemId(orderItemId);

        kitchenDAO.updateStatus(
                orderItemId,
                KitchenStatus.READY
        );
    }

    private void validateOrderItemId(
            long orderItemId
    ) {
        if (orderItemId <= 0) {
            throw new IllegalArgumentException(
                    "Mã món cần xử lý không hợp lệ"
            );
        }
    }
}