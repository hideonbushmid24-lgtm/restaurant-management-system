package com.restaurant.model;

import java.math.BigDecimal;

public class MenuItem {

    private final long id;
    private final String itemName;
    private final String category;
    private final String description;
    private final BigDecimal price;
    private final boolean available;

    public MenuItem(
            long id,
            String itemName,
            String category,
            String description,
            BigDecimal price,
            boolean available
    ) {
        this.id = id;
        this.itemName = itemName;
        this.category = category;
        this.description = description;
        this.price = price;
        this.available = available;
    }

    public long getId() {
        return id;
    }

    public String getItemName() {
        return itemName;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isAvailable() {
        return available;
    }

    @Override
    public String toString() {
        return itemName;
    }
}