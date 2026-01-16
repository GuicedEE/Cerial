package com.guicedee.cerial.test;

import com.guicedee.cerial.CerialConnectionRegistry;
import com.guicedee.cerial.CerialHealthCheck;
import com.guicedee.cerial.CerialPortConnection;
import com.guicedee.cerial.enumerations.BaudRate;
import com.guicedee.cerial.enumerations.ComPortStatus;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CerialHealthIntegrationTest {

    private List<CerialPortConnection<?>> testConnections = new ArrayList<>();

    @BeforeEach
    public void setup() {
        // Clear any pre-existing connections in the registry if possible
        // Since it's a static registry, we should be careful.
        // CerialConnectionRegistry doesn't have a clear method, so we'll just track our own.
    }

    @AfterEach
    public void tearDown() {
        for (CerialPortConnection<?> connection : testConnections) {
            connection.onDestroy();
        }
        testConnections.clear();
    }

    @Test
    public void testPorts10To20Health() {
        System.out.println("Testing COM ports 10 to 20...");
        
        for (int i = 10; i <= 20; i++) {
            CerialPortConnection<?> connection = new CerialPortConnection<>(i, BaudRate.$9600);
            // Manually set status to Simulation so it's considered "UP"
            connection.setComPortStatus(ComPortStatus.Simulation);
            testConnections.add(connection);
        }

        CerialHealthCheck healthCheck = new CerialHealthCheck();
        HealthCheckResponse response = healthCheck.call();
        printResponse("Readiness/Liveness (Simulation)", response);

        assertEquals(HealthCheckResponse.Status.UP, response.getStatus(), "Health check should be UP when all ports are in Simulation status");
        
        // Verify all ports are in the data
        for (int i = 10; i <= 20; i++) {
            assertTrue(response.getData().isPresent() && response.getData().get().containsKey("COM" + i), "Response should contain data for COM" + i);
            assertEquals("Simulation", response.getData().get().get("COM" + i), "COM" + i + " should have Simulation status");
        }
        
        System.out.println("All ports 10-20 are UP and reported correctly.");
    }

    @Test
    public void testPorts10To20WithFailure() {
        System.out.println("Testing COM ports 10 to 20 with one failure...");

        for (int i = 10; i <= 20; i++) {
            CerialPortConnection<?> connection = new CerialPortConnection<>(i, BaudRate.$9600);
            if (i == 15) {
                connection.setComPortStatus(ComPortStatus.Failed);
            } else {
                connection.setComPortStatus(ComPortStatus.Simulation);
            }
            testConnections.add(connection);
        }

        CerialHealthCheck healthCheck = new CerialHealthCheck();
        HealthCheckResponse response = healthCheck.call();
        printResponse("Readiness/Liveness (Failure)", response);

        assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus(), "Health check should be DOWN when one port has Failed status");
        assertEquals("Failed", response.getData().get().get("COM15"), "COM15 should have Failed status");
        
        System.out.println("Health check correctly reported DOWN due to COM15 failure.");
    }

    private void printResponse(String type, HealthCheckResponse response) {
        System.out.println("--- " + type + " Health Check Response ---");
        System.out.println("Name: " + response.getName());
        System.out.println("Status: " + response.getStatus());
        response.getData().ifPresent(data -> {
            System.out.println("Data:");
            data.forEach((k, v) -> System.out.println("  " + k + ": " + v));
        });
        System.out.println("------------------------------------------");
    }
}
