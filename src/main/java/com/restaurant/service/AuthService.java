package com.restaurant.service;

import com.restaurant.dao.UserRepository;
import com.restaurant.model.User;
import com.restaurant.util.PasswordUtil;

import java.sql.SQLException;
import java.util.Optional;

public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        if (userRepository == null) {
            throw new IllegalArgumentException(
                    "UserRepository không được null"
            );
        }

        this.userRepository = userRepository;
    }

    public Optional<User> login(
            String username,
            String password
    ) throws SQLException {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Tên đăng nhập không được để trống"
            );
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "Mật khẩu không được để trống"
            );
        }

        Optional<User> userOptional =
                userRepository.findActiveByUsername(
                        username.trim()
                );

        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        User user = userOptional.get();

        if (!PasswordUtil.matches(
                password,
                user.getPasswordHash()
        )) {
            return Optional.empty();
        }

        return Optional.of(user);
    }
}