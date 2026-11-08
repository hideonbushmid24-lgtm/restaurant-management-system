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
        /*
         * Cho phép ghi đè cấu hình bằng tham số -D khi chạy
         * (ví dụ -Ddb.url=...). Nhờ vậy test có thể dùng một
         * database riêng mà không đụng vào dữ liệu thật.
         */
        String url = readSetting("db.url");
        String username = readSetting("db.username");
        String password = readSetting("db.password");

        if (url == null || username == null || password == null) {
            throw new IllegalStateException(
                    "Cấu hình database chưa đầy đủ"
            );
        }

        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Đọc cấu hình: ưu tiên tham số -D, sau đó mới đến file
     * application.properties.
     */
    private static String readSetting(String key) {
        String override = System.getProperty(key);

        if (override != null && !override.isBlank()) {
            return override;
        }

        return PROPERTIES.getProperty(key);
    }
}