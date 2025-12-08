// src/main/java/com/odp/simulator/client/service/OdpConnectionService.java
package com.odp.simulator.client.service;

import com.odp.simulator.client.client.OdpClientManager;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for managing ODP connection lifecycle
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OdpConnectionService {

    private final OdpClientManager clientManager;

    /**
     * Connect to ODP gateway and establish session
     */
    public void connectAndLogon() throws Exception {
        clientManager.connectAndLogon();
    }

    /**
     * Check if connected and ready for trading
     */
    public boolean isReady() {
        return clientManager.isReady();
    }

    /**
     * Disconnect from gateway
     */
    @PreDestroy
    public void disconnect() {
        log.info("Shutting down ODP connection...");
        clientManager.disconnect();
    }
}