package com.restaurant.service;

import com.restaurant.config.DatabaseConnection;
import com.restaurant.dao.TableDAO;
import com.restaurant.model.RestaurantTable;
import com.restaurant.model.TableStatus;
import com.restaurant.support.TestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Kiểm thử nghiệp vụ sửa bàn với database thật.
 * Chỉ chạy khi có -Ddb.url trỏ tới database *_test.
 */
class TableServiceIntegrationTest {

    private TableService tableService;

    @BeforeEach
    void setUp() throws SQLException {
        TestDatabase.requireTestDatabase();
        TestDatabase.resetBusinessData();
        tableService = new TableService(new TableDAO());
    }

    // ---------- Hàm hỗ trợ tạo dữ liệu ----------

    private long createTable(String number, int capacity, String status)
            throws SQLException {
        TestDatabase.execute(
                "INSERT INTO restaurant_tables "
                        + "(table_number, capacity, status, active) VALUES ('"
                        + number + "', " + capacity + ", '" + status + "', TRUE)"
        );
        return queryLong(
                "SELECT id FROM restaurant_tables WHERE table_number = '"
                        + number + "'"
        );
    }

    private void createReservation(long tableId, int guests, String status)
            throws SQLException {
        /*
         * Mỗi lượt đặt dùng một khách riêng (số điện thoại theo mã bàn
         * và số lượt đã có) để không vi phạm ràng buộc UNIQUE.
         */
        long count = queryLong("SELECT COUNT(*) FROM customers");
        long customerId = TestDatabase.createCustomer(
                "Khach Test " + count, "09000000" + (10 + count)
        );

        TestDatabase.execute(
                "INSERT INTO reservations (table_id, customer_id, "
                        + "reservation_time, guest_count, status, "
                        + "created_by) VALUES (" + tableId + ", "
                        + customerId + ", NOW(), " + guests + ", '"
                        + status + "', (SELECT id FROM users "
                        + "WHERE username = 'receptionist'))"
        );
    }

    private void createOrder(long tableId, String status) throws SQLException {
        TestDatabase.execute(
                "INSERT INTO orders (table_id, waiter_id, status) VALUES ("
                        + tableId + ", (SELECT id FROM users "
                        + "WHERE username = 'waiter'), '" + status + "')"
        );
    }

    private long queryLong(String sql) throws SQLException {
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private RestaurantTable load(String number) throws SQLException {
        return tableService.search(number, null).get(0);
    }

    // ---------- Các trường hợp kiểm thử ----------

    @Test
    void khongChoDoiBanDangCoDonHoatDongVeTrong() throws SQLException {
        long id = createTable("T01", 4, "OCCUPIED");
        createOrder(id, "OPEN");

        assertThrows(
                IllegalArgumentException.class,
                () -> tableService.updateTable(
                        id, "T01", 4, TableStatus.AVAILABLE
                )
        );

        assertEquals(TableStatus.OCCUPIED, load("T01").getStatus());
    }

    @Test
    void khongChoDoiBanDaDatVeTrongKhiLuotDatConHieuLuc()
            throws SQLException {
        long id = createTable("T02", 4, "RESERVED");
        createReservation(id, 3, "CONFIRMED");

        assertThrows(
                IllegalArgumentException.class,
                () -> tableService.updateTable(
                        id, "T02", 4, TableStatus.AVAILABLE
                )
        );

        assertEquals(TableStatus.RESERVED, load("T02").getStatus());
    }

    @Test
    void khongChoHaSucChuaDuoiSoKhachCuaLuotDat() throws SQLException {
        long id = createTable("T03", 6, "RESERVED");
        createReservation(id, 5, "CONFIRMED");

        assertThrows(
                IllegalArgumentException.class,
                () -> tableService.updateTable(
                        id, "T03", 4, TableStatus.RESERVED
                )
        );

        assertEquals(6, load("T03").getCapacity());
    }

    @Test
    void choSuaSoBanVaTangSucChuaKhiBanDangDung() throws SQLException {
        long id = createTable("T04", 4, "OCCUPIED");
        createOrder(id, "IN_KITCHEN");

        tableService.updateTable(
                id, "T04A", 6, TableStatus.OCCUPIED
        );

        RestaurantTable table = load("T04A");
        assertEquals(6, table.getCapacity());
        assertEquals(TableStatus.OCCUPIED, table.getStatus());
    }

    @Test
    void khongChoDatTrangThaiDaDatKhiKhongCoLuotDat() throws SQLException {
        long id = createTable("T05", 4, "AVAILABLE");

        assertThrows(
                IllegalArgumentException.class,
                () -> tableService.updateTable(
                        id, "T05", 4, TableStatus.RESERVED
                )
        );

        assertEquals(TableStatus.AVAILABLE, load("T05").getStatus());
    }

    @Test
    void khongChoDatTrangThaiDangDungKhiKhongCoDon() throws SQLException {
        long id = createTable("T06", 4, "AVAILABLE");

        assertThrows(
                IllegalArgumentException.class,
                () -> tableService.updateTable(
                        id, "T06", 4, TableStatus.OCCUPIED
                )
        );

        assertEquals(TableStatus.AVAILABLE, load("T06").getStatus());
    }

    @Test
    void choGoiTrangThaiBiKetVeTrongKhiKhongConDonHayLuotDat()
            throws SQLException {
        long id = createTable("T07", 4, "OCCUPIED");
        createOrder(id, "PAID");

        tableService.updateTable(
                id, "T07", 4, TableStatus.AVAILABLE
        );

        assertEquals(TableStatus.AVAILABLE, load("T07").getStatus());
    }

    @Test
    void luotDatDaHuyHoacHoanTatKhongChanDoiTrangThai()
            throws SQLException {
        long id = createTable("T08", 4, "RESERVED");
        createReservation(id, 2, "CANCELLED");
        createReservation(id, 2, "COMPLETED");

        tableService.updateTable(
                id, "T08", 2, TableStatus.AVAILABLE
        );

        RestaurantTable table = load("T08");
        assertEquals(TableStatus.AVAILABLE, table.getStatus());
        assertEquals(2, table.getCapacity());
    }
}
