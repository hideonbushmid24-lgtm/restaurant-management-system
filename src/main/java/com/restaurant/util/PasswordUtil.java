package com.restaurant.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hash(String password) {
        if (password == null) {
            throw new IllegalArgumentException(
                    "Mật khẩu không được để trống"
            );
        }

        try {
            MessageDigest messageDigest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = messageDigest.digest(
                    password.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "Máy không hỗ trợ SHA-256",
                    exception
            );
        }
    }

    public static boolean matches(
            String rawPassword,
            String storedHash
    ) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }

        return hash(rawPassword).equalsIgnoreCase(storedHash);
    }
}