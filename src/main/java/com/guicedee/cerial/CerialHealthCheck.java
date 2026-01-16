package com.guicedee.cerial;

import com.guicedee.cerial.enumerations.ComPortStatus;
import org.eclipse.microprofile.health.*;

import com.google.inject.Singleton;
import java.util.Set;

/**
 * Health check for Cerial connections.
 * Reports UP if all active connections are in a healthy state, DOWN otherwise.
 */
@Liveness
@Readiness
@Startup
@Singleton
public class CerialHealthCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Cerial Connections");
        Set<CerialPortConnection<?>> connections = CerialConnectionRegistry.getActiveConnections();

        if (connections.isEmpty()) {
            return builder.up().withData("status", "No active connections").build();
        }

        boolean allUp = true;
        for (CerialPortConnection<?> connection : connections) {
            String portName = "COM" + connection.getComPort();
            ComPortStatus status = connection.getComPortStatus();
            builder.withData(portName, status.name());

            // Define which statuses are considered "UP" for health check
            if (status == ComPortStatus.Offline || 
                status == ComPortStatus.Missing || 
                status == ComPortStatus.GeneralException || 
                status == ComPortStatus.Failed ||
                status == ComPortStatus.InUse) {
                allUp = false;
            }
        }

        return allUp ? builder.up().build() : builder.down().build();
    }
}
