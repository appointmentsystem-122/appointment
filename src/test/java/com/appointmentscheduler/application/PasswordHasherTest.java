package com.appointmentscheduler.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;

class PasswordHasherTest {

    @Test
    void hash_verify_roundTrip() {
        String hash = PasswordHasher.hash("SecretP@ss1");
        assertTrue(PasswordHasher.verify("SecretP@ss1", hash));
        assertFalse(PasswordHasher.verify("wrong", hash));
    }

    @Test
    void hash_nullOrEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(""));
    }

    @Test
    void verify_nullInputs_false() {
        assertFalse(PasswordHasher.verify(null, "$2a$dummy"));
        assertFalse(PasswordHasher.verify("x", null));
    }

    @Test
    void hash_producesBcryptPrefix() {
        assertThat(PasswordHasher.hash("pw")).startsWith("$2");
    }

    @Test
    void utilityConstructor_isAccessibleForCoverage() throws Exception {
        Constructor<PasswordHasher> ctor = PasswordHasher.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        PasswordHasher instance = ctor.newInstance();
        assertThat(instance).isNotNull();
    }
}
