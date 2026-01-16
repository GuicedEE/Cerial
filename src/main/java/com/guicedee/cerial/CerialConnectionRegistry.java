package com.guicedee.cerial;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry to keep track of active CerialPortConnection instances for health reporting.
 */
public class CerialConnectionRegistry {
    private static final Set<CerialPortConnection<?>> activeConnections = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Registers a connection.
     *
     * @param connection The connection to register.
     */
    public static void register(CerialPortConnection<?> connection) {
        activeConnections.add(connection);
    }

    /**
     * Unregisters a connection.
     *
     * @param connection The connection to unregister.
     */
    public static void unregister(CerialPortConnection<?> connection) {
        activeConnections.remove(connection);
    }

    /**
     * Returns an unmodifiable set of active connections.
     *
     * @return The set of active connections.
     */
    public static Set<CerialPortConnection<?>> getActiveConnections() {
        return Collections.unmodifiableSet(activeConnections);
    }
}
