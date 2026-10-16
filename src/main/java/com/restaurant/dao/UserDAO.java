package com.restaurant.dao;

import com.restaurant.config.DatabaseConnection;
import com.restaurant.model.Role;
import com.restaurant.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserDAO implements UserRepository {

    private static final String FIND_ACTIVE_USER_SQL = """
            SELECT
                id,
                username,
                password_hash,
                full_name,
                role,
                active
            FROM users
            WHERE username = ?
              AND active = TRUE
            """;

    @Override
    public Optional<User> findActiveByUsername(String username)
            throws SQLException {

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                FIND_ACTIVE_USER_SQL
                        )
        ) {
            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                User user = new User(
                        resultSet.getLong("id"),
                        resultSet.getString("username"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("full_name"),
                        Role.valueOf(
                                resultSet.getString("role")
                        ),
                        resultSet.getBoolean("active")
                );

                return Optional.of(user);
            }
        }
    }
}