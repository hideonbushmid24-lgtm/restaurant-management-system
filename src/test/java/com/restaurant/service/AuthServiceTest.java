package com.restaurant.service;

import com.restaurant.dao.UserRepository;
import com.restaurant.model.Role;
import com.restaurant.model.User;
import com.restaurant.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        User testUser = new User(
                1L,
                "manager",
                PasswordUtil.hash("manager123"),
                "Quản lý nhà hàng",
                Role.MANAGER,
                true
        );

        UserRepository fakeRepository = username -> {
            if ("manager".equals(username)) {
                return Optional.of(testUser);
            }

            return Optional.empty();
        };

        authService = new AuthService(fakeRepository);
    }

    @Test
    void loginReturnsUserWhenCredentialsAreCorrect()
            throws SQLException {

        Optional<User> result =
                authService.login(
                        "manager",
                        "manager123"
                );

        assertTrue(result.isPresent());
        assertEquals(
                Role.MANAGER,
                result.get().getRole()
        );
    }

    @Test
    void loginReturnsEmptyWhenPasswordIsWrong()
            throws SQLException {

        Optional<User> result =
                authService.login(
                        "manager",
                        "wrong-password"
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void loginReturnsEmptyWhenUsernameDoesNotExist()
            throws SQLException {

        Optional<User> result =
                authService.login(
                        "unknown",
                        "manager123"
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void loginRejectsBlankUsername() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> authService.login(
                                " ",
                                "manager123"
                        )
                );

        assertEquals(
                "Tên đăng nhập không được để trống",
                exception.getMessage()
        );
    }

    @Test
    void loginRejectsBlankPassword() {
        assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(
                        "manager",
                        " "
                )
        );
    }
}