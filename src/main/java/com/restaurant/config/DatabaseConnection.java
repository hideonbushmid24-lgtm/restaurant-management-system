package com.restaurant.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Tạo kết nối tới database.
 *
 * Mỗi thông số (db.url, db.username, db.password) được lấy theo thứ tự
 * ưu tiên từ cao xuống thấp:
 *   1. Tham số khi chạy:        -Ddb.url=...
 *   2. Biến môi trường:         DB_URL, DB_USERNAME, DB_PASSWORD
 *   3. File riêng của từng máy: config/local.properties
 *      (không đưa lên GitHub, xem config/local.properties.example)
 *   4. File mặc định demo:      src/main/resources/application.properties
 *
 * Nhờ vậy thông tin kết nối thật không cần nằm trong mã nguồn.
 */
public final class DatabaseConnection {

    private static final String DEFAULT_CONFIG_FILE =
            "application.properties";

    private static final Path LOCAL_CONFIG_FILE =
            Path.of("config", "local.properties");

    private static final Properties DEFAULTS = loadDefaults();
    private static final Properties LOCAL = loadLocal();

    private DatabaseConnection() {
    }

    private static Properties loadDefaults() {
        Properties properties = new Properties();

        try (InputStream input = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream(DEFAULT_CONFIG_FILE)) {

            if (input == null) {
                throw new IllegalStateException(
                        "Không tìm thấy file " + DEFAULT_CONFIG_FILE
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

    /** Đọc file riêng của máy nếu có; không có thì bỏ qua. */
    private static Properties loadLocal() {
        Properties properties = new Properties();

        if (!Files.isReadable(LOCAL_CONFIG_FILE)) {
            return properties;
        }

        try (InputStream input =
                     Files.newInputStream(LOCAL_CONFIG_FILE)) {

            properties.load(input);
            return properties;

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Không thể đọc file " + LOCAL_CONFIG_FILE,
                    exception
            );
        }
    }

    public static Connection getConnection() throws SQLException {
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
     * Đọc một thông số cấu hình theo thứ tự ưu tiên đã nêu ở đầu lớp.
     * Giá trị rỗng được coi như chưa đặt.
     */
    static String readSetting(String key) {
        String fromProperty = System.getProperty(key);

        if (isSet(fromProperty)) {
            return fromProperty;
        }

        // db.url -> DB_URL
        String fromEnvironment = System.getenv(
                key.toUpperCase().replace('.', '_')
        );

        if (isSet(fromEnvironment)) {
            return fromEnvironment;
        }

        String fromLocalFile = LOCAL.getProperty(key);

        if (isSet(fromLocalFile)) {
            return fromLocalFile;
        }

        return DEFAULTS.getProperty(key);
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }
}
