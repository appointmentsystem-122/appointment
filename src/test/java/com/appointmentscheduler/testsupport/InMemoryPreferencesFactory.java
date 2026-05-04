package com.appointmentscheduler.testsupport;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * Test-only Preferences backend. It keeps unit tests independent from the host OS
 * registry or user profile permissions.
 */
public final class InMemoryPreferencesFactory implements PreferencesFactory {

    private static final Preferences USER_ROOT = new MemoryPreferences(null, "");
    private static final Preferences SYSTEM_ROOT = new MemoryPreferences(null, "");

    @Override
    public Preferences userRoot() {
        return USER_ROOT;
    }

    @Override
    public Preferences systemRoot() {
        return SYSTEM_ROOT;
    }

    private static final class MemoryPreferences extends AbstractPreferences {
        private final Map<String, String> values = new HashMap<>();
        private final Map<String, MemoryPreferences> children = new HashMap<>();

        private MemoryPreferences(AbstractPreferences parent, String name) {
            super(parent, name);
        }

        @Override
        protected void putSpi(String key, String value) {
            values.put(key, value);
        }

        @Override
        protected String getSpi(String key) {
            return values.get(key);
        }

        @Override
        protected void removeSpi(String key) {
            values.remove(key);
        }

        @Override
        protected void removeNodeSpi() {
            values.clear();
            children.clear();
        }

        @Override
        protected String[] keysSpi() {
            return values.keySet().toArray(String[]::new);
        }

        @Override
        protected String[] childrenNamesSpi() {
            return children.keySet().toArray(String[]::new);
        }

        @Override
        protected AbstractPreferences childSpi(String name) {
            Objects.requireNonNull(name, "name");
            return children.computeIfAbsent(name, child -> new MemoryPreferences(this, child));
        }

        @Override
        protected void syncSpi() throws BackingStoreException {
            // In-memory store is always current.
        }

        @Override
        protected void flushSpi() throws BackingStoreException {
            // Nothing to flush.
        }
    }
}
