package com.restaurant.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConnection {

    private static final String CONFIG_FILE = "application.properties";
    private static final Properties PROPERTIES = loadProperties();

    private DatabaseConnection() {
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();

        try (InputStream input = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {

            if (input == null) {
                throw new IllegalStateException(
                        "Không tìm thấy file " + CONFIG_FILE
                );
            }

            properties.load(input);
            return properties;

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Không thể đọc cấu hình database",
                    exception
            );
        }
    }

    public static Connection getConnection() throws SQLException {
        String url = PROPERTIES.getProperty("db.url");
        String username = PROPERTIES.getProperty("db.username");
        String password = PROPERTIES.getProperty("db.password");

        if (url == null || username == null || password == null) {
            throw new IllegalStateException(
                    "Cấu hình database chưa đầy đủ"
            );
        }

        return DriverManager.getConnection(url, username, password);
    }
}