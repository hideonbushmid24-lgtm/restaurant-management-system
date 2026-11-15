package com.restaurant.ui;

import com.restaurant.dao.MenuDAO;
import com.restaurant.dao.OrderDAO;
import com.restaurant.dao.TableDAO;
import com.restaurant.dao.UserDAO;
import com.restaurant.model.User;
import com.restaurant.service.MenuService;
import com.restaurant.service.OrderService;
import com.restaurant.service.TableService;
import com.restaurant.support.TestDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử bố cục màn hình gọi món (OrderFrame).
 *
 * Tự bỏ qua khi máy không có màn hình (CI không giao diện) hoặc chưa cấu hình
 * database *_test, giống các test nghiệp vụ khác.
 */
class OrderFrameLayoutTest {

    private OrderFrame frame;

    @BeforeEach
    void moManHinh() throws Exception {
        Assumptions.assumeFalse(
                GraphicsEnvironment.isHeadless(),
                "Cần môi trường có màn hình để dựng giao diện Swing"
        );
        TestDatabase.requireTestDatabase();
        TestDatabase.resetBusinessData();
        TestDatabase.execute(
                "INSERT INTO restaurant_tables(table_number, capacity) "
                        + "VALUES ('B01', 4)"
        );
        TestDatabase.execute(
                "INSERT INTO menu_items(item_name, category, price) "
                        + "VALUES ('Mì xào', 'Món chính', 79000)"
        );

        User waiter =
                new UserDAO().findActiveByUsername("waiter").orElseThrow();

        SwingUtilities.invokeAndWait(() -> {
            frame = new OrderFrame(
                    new OrderService(new OrderDAO()),
                    new MenuService(new MenuDAO()),
                    new TableService(new TableDAO()),
                    waiter
            );
            frame.setVisible(true);
            frame.validate();
        });
    }

    @AfterEach
    void dongManHinh() throws Exception {
        if (frame == null) {
            return;
        }
        SwingUtilities.invokeAndWait(() -> {
            for (Window window : Window.getWindows()) {
                window.dispose();
            }
        });
    }

    private List<JButton> buttonsWithText() {
        List<JButton> result = new ArrayList<>();
        collect(frame, result);
        return result;
    }

    private static void collect(Container container, List<JButton> out) {
        for (Component child : container.getComponents()) {
            if (child instanceof JButton button
                    && button.getText() != null
                    && !button.getText().isBlank()) {
                out.add(button);
            }
            if (child instanceof Container nested) {
                collect(nested, out);
            }
        }
    }

    private JButton button(String text) {
        return buttonsWithText().stream()
                .filter(b -> text.equals(b.getText()))
                .findFirst()
                .orElse(null);
    }

    @Test
    void coNutXemThucDonVaMoDuocManHinhThucDon() throws Exception {
        JButton viewMenu = button("Xem thực đơn");

        assertNotNull(
                viewMenu,
                "Màn hình gọi món phải có nút \"Xem thực đơn\" "
                        + "(nếu thiếu thì không mở được MenuFrame)"
        );
        assertTrue(viewMenu.isEnabled());

        SwingUtilities.invokeAndWait(viewMenu::doClick);

        boolean menuOpened = false;
        for (Window window : Window.getWindows()) {
            if (window instanceof MenuFrame && window.isShowing()) {
                menuOpened = true;
            }
        }
        assertTrue(menuOpened, "Bấm \"Xem thực đơn\" phải mở MenuFrame");
    }

    @Test
    void chuaCoDonThiKhoaCacNutThaoTacTrenMon() {
        for (String text : List.of(
                "Thêm món",
                "Tăng số lượng",
                "Giảm số lượng",
                "Hủy món",
                "Gửi xuống bếp",
                "Xác nhận đã phục vụ"
        )) {
            JButton b = button(text);
            assertNotNull(b, "Thiếu nút " + text);
            assertFalse(
                    b.isEnabled(),
                    "Nút \"" + text + "\" phải bị khóa khi chưa mở đơn"
            );
        }

        assertTrue(button("Tạo hoặc mở đơn").isEnabled());
    }

    @Test
    void khongNutNaoBiCatMatKhiMoCuaSoMacDinh() {
        for (JButton b : buttonsWithText()) {
            Container parent = b.getParent();
            Rectangle bounds = b.getBounds();

            assertTrue(
                    bounds.y >= 0
                            && bounds.y + bounds.height <= parent.getHeight()
                            && bounds.x >= 0
                            && bounds.x + bounds.width <= parent.getWidth(),
                    "Nút \"" + b.getText() + "\" bị cắt mất: nút "
                            + bounds + " nằm ngoài vùng hiển thị của khung chứa "
                            + parent.getSize()
            );
        }
    }
}
