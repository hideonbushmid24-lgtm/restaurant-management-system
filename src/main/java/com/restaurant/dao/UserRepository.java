package com.restaurant.dao;

import com.restaurant.model.User;

import java.sql.SQLException;
import java.util.Optional;

@FunctionalInterface
public interface UserRepository {

    Optional<User> findActiveByUsername(String username)
            throws SQLException;
}