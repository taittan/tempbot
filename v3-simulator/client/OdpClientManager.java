// src/main/java/com/odp/simulator/client/client/OdpClientManager.java
package com.odp.simulator.client.client;

import com.odp.simulator.client.protocol.messages.LookupResponse;
import com.odp.simulator.client.protocol.messages.LogonResponse;
import com.odp.simulator.client.session.OdpSession;
import com.odp.simulator.client.session.OdpSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Manager for ODP client operations
 * 
 * Coordinates lookup, connection, and trading operations.
 * 
 * Order Processing Design (TODO):
 * ---------------------------------
 * Orders come in continuously and need to be processed efficiently.
 * 
 * Design considerations:
 * 1. Single Channel: Netty uses a single channel for all messages.
 *    No need to open different channels for different orders.
 * 
 * 2. Handler Sharing: The same handler processes all messages.
 *    Message routing is based on message type, not order.
 * 
 * 3. Order Tracking: Orders are tracked by Client Order ID.
 *    A map of Client Order ID to Order state should be maintained.
 * 
 * 4. Execution Reports: Multiple execution reports may arrive for
 *    a single order (partial fills, etc.). These are identified
 *    by the Order ID or Client Order ID in the message.
 * 
 * 5. Async Processing: Order responses are processed asynchronously.
 *    CompletableFuture or callbacks can be used to notify callers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OdpClientManager {

    private final OdpLookupClient lookupClient;
    private final OdpTradingClient tradingClient;
    private final OdpSessionManager sessionManager;

    /**
     * Perform full connection sequence:
     * 1. Lookup to get gateway address
     * 2. Connect to gateway
     * 3. Logon
     */
    public void connectAndLogon() throws Exception {
        log.info("Starting ODP connection sequence...");

        // Step 1: Lookup
        log.info("Step 1: Performing lookup...");
        LookupResponse lookupResponse = lookupClient.performLookup();
        
        if (!lookupResponse.isAccepted()) {
            throw new RuntimeException("Lookup failed: " + lookupResponse.getRejectReasonDescription());
        }

        // Step 2 & 3: Connect and Logon
        log.info("Step 2: Connecting to gateway and logging on...");
        OdpSession session = sessionManager.getPrimarySession();
        
        // Use primary gateway address
        String host = session.getGatewayIpPrimary();
        int port = session.getGatewayPortPrimary();
        
        LogonResponse logonResponse = tradingClient.connectAndLogon(host, port);
        
        if (!logonResponse.isSessionActive()) {
            throw new RuntimeException("Logon failed: " + logonResponse.getSessionStatusDescription());
        }

        log.info("ODP connection sequence completed successfully");
        log.info("Session mode: {}", logonResponse.isTestMode() ? "Test" : "Production");
    }

    /**
     * Check if the client is connected and logged on
     */
    public boolean isReady() {
        OdpSession session = sessionManager.getPrimarySession();
        return session.isActive() && tradingClient.isConnected();
    }

    /**
     * Disconnect from the gateway
     */
    public void disconnect() {
        log.info("Disconnecting from ODP gateway...");
        tradingClient.disconnect();
        sessionManager.closeAllSessions();
    }
}