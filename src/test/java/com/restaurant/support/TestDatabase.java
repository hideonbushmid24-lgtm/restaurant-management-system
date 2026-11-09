package com.restaurant.support;

import com.restaurant.config.DatabaseConnection;
import org.junit.jupiter.api.Assumptions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Hỗ trợ test tích hợp với database thật.
 *
 * An toàn dữ liệu: test chỉ chạy khi tên database kết thúc
 * bằng "_test" (ví dụ restaurant_test). Nếu không, test bị bỏ qua
 * để không bao giờ xóa nhầm dữ liệu demo.
 *
 * Cách chạy:
 *   -Ddb.url=jdbc:mariadb://localhost:3306/restaurant_test
 */
public final class TestDatabase {

    private TestDatabase() {
    }

    /** Bỏ qua test nếu chưa cấu hình database test hợp lệ. */
    public static void requireTestDatabase() {
        String url = System.getProperty("db.url");

        Assumptions.assumeTrue(
                url != null && url.matches(".*/[A-Za-z0-9_]*_test(\\?.*)?$"),
                "Bỏ qua: cần -Ddb.url trỏ tới database có tên kết thúc bằng _test"
        );
    }

    /** Xóa dữ liệu nghiệp vụ, giữ lại tài khoản người dùng. */
    public static void resetBusinessData() throws SQLException {
        try (
                Connection connection =
                        DatabaseConnection.getConnection();
                Statement statement =
                        connection.createStatement()
        ) {
            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            statement.execute("TRUNCATE TABLE invoices");
            statement.execute("TRUNCATE TABLE payments");
            statement.execute("TRUNCATE TABLE order_items");
            statement.execute("TRUNCATE TABLE orders");
            statement.execute("TRUNCATE TABLE reservations");
            statement.execute("TRUNCATE TABLE customers");
            statement.execute("TRUNCATE TABLE restaurant_tables");
            statement.execute("TRUNCATE TABLE menu_items");
            statement.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    /** Chạy một câu lệnh SQL chuẩn bị dữ liệu cho test. */
    public static void execute(String sql) throws SQLException {
        try (
                Connection connection =
                        DatabaseConnection.getConnection();
                Statement statement =
                        connection.createStatement()
        ) {
            statement.execute(sql);
        }
    }

    /** Chạy câu SELECT trả về một số nguyên (ví dụ COUNT hoặc id). */
    public static long queryLong(String sql) throws SQLException {
        try (
                Connection connection =
                        DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    /** Chạy câu SELECT trả về một chuỗi (ví dụ trạng thái). */
    public static String queryString(String sql) throws SQLException {
        try (
                Connection connection =
                        DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            return resultSet.next() ? resultSet.getString(1) : null;
        }
    }

    /** Tạo sẵn một khách hàng, trả về mã khách. */
    public static long createCustomer(String fullName, String phone)
            throws SQLException {
        execute(
                "INSERT INTO customers (full_name, phone) VALUES ('"
                        + fullName + "', '" + phone + "')"
        );
        return queryLong(
                "SELECT id FROM customers WHERE phone = '" + phone + "'"
        );
    }
}
