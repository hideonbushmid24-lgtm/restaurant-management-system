package com.restaurant.service;

import com.restaurant.dao.MenuDAO;
import com.restaurant.model.MenuItem;

import java.sql.SQLException;
import java.util.List;

public class MenuService {

    private final MenuDAO menuDAO;

    public MenuService(MenuDAO menuDAO) {
        if (menuDAO == null) {
            throw new IllegalArgumentException(
                    "MenuDAO không được null"
            );
        }

        this.menuDAO = menuDAO;
    }

    /**
     * Tìm kiếm các món đang được bán.
     */
    public List<MenuItem> searchAvailableItems(
            String keyword,
            String category
    ) throws SQLException {

        String normalizedKeyword =
                normalizeKeyword(keyword);

        String normalizedCategory =
                normalizeCategory(category);

        return menuDAO.search(
                normalizedKeyword,
                normalizedCategory,
                true
        );
    }

    /**
     * Lấy danh sách danh mục đang có món được bán.
     */
    public List<String> getAvailableCategories()
            throws SQLException {

        return menuDAO.findCategories();
    }

    /**
     * Lấy thông tin một món đang được bán.
     */
    public MenuItem getAvailableItem(long id)
            throws SQLException {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Mã món ăn không hợp lệ"
            );
        }

        MenuItem menuItem =
                menuDAO.findAvailableById(id);

        if (menuItem == null) {
            throw new IllegalArgumentException(
                    "Món ăn không tồn tại hoặc đã ngừng bán"
            );
        }

        return menuItem;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null
                || keyword.isBlank()) {

            return null;
        }

        String normalized = keyword.trim();

        if (normalized.length() > 150) {
            throw new IllegalArgumentException(
                    "Từ khóa tìm kiếm không được vượt quá 150 ký tự"
            );
        }

        return normalized;
    }

    private String normalizeCategory(String category) {
        if (category == null
                || category.isBlank()
                || "Tất cả".equalsIgnoreCase(
                        category.trim()
                )) {

            return null;
        }

        return category.trim();
    }
}