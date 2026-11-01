package com.restaurant.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void hashSamePasswordProducesSameResult() {
        String firstHash = PasswordUtil.hash("manager123");
        String secondHash = PasswordUtil.hash("manager123");

        assertEquals(firstHash, secondHash);
        assertEquals(64, firstHash.length());
    }

    @Test
    void matchesReturnsTrueForCorrectPassword() {
        String hash = PasswordUtil.hash("manager123");

        assertTrue(
                PasswordUtil.matches("manager123", hash)
        );
    }

    @Test
    void matchesReturnsFalseForWrongPassword() {
        String hash = PasswordUtil.hash("manager123");

        assertFalse(
                PasswordUtil.matches("wrong-password", hash)
        );
    }
}