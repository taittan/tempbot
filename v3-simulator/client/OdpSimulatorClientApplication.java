// src/main/java/com/odp/simulator/client/OdpSimulatorClientApplication.java
package com.odp.simulator.client;

import com.odp.simulator.client.service.OdpConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class OdpSimulatorClientApplication implements CommandLineRunner {

    private final OdpConnectionService connectionService;

    public static void main(String[] args) {
        SpringApplication.run(OdpSimulatorClientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting ODP Simulator Client...");
        
        try {
            // Step 1: Lookup to get gateway connection point
            // Step 2: Connect to gateway and logon
            connectionService.connectAndLogon();
            
            log.info("ODP Simulator Client started successfully");
        } catch (Exception e) {
            log.error("Failed to start ODP Simulator Client", e);
            throw e;
        }
    }
}