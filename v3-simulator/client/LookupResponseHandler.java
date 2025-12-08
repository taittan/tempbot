// src/main/java/com/odp/simulator/client/handler/LookupResponseHandler.java
package com.odp.simulator.client.handler;

import com.odp.simulator.client.protocol.OdpMessage;
import com.odp.simulator.client.protocol.OdpMessageType;
import com.odp.simulator.client.protocol.messages.LookupResponse;
import com.odp.simulator.client.session.OdpSession;
import com.odp.simulator.client.session.OdpSessionManager;
import com.odp.simulator.client.session.OdpSessionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Handler for Lookup Response messages
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LookupResponseHandler implements OdpMessageHandler {

    private final OdpSessionManager sessionManager;
    
    // Used to signal completion of lookup
    private volatile CompletableFuture<LookupResponse> lookupFuture;

    @Override
    public OdpMessageType getMessageType() {
        return OdpMessageType.LOOKUP_RESPONSE;
    }

    @Override
    public void handle(OdpMessage message) {
        if (!(message instanceof LookupResponse response)) {
            log.error("Expected LookupResponse but got: {}", message.getClass().getSimpleName());
            return;
        }

        log.info("Received Lookup Response - Status: {}", 
                response.isAccepted() ? "Accepted" : "Rejected");

        OdpSession session = sessionManager.getPrimarySession();

        if (response.isAccepted()) {
            // Store gateway connection info in session
            session.setGatewayIpPrimary(response.getIpAddress1());
            session.setGatewayPortPrimary(response.getPortNumber1());
            session.setGatewayIpSecondary(response.getIpAddress2());
            session.setGatewayPortSecondary(response.getPortNumber2());
            session.transitionTo(OdpSessionState.LOOKUP_COMPLETE);

            log.info("Gateway Primary: {}:{}", response.getIpAddress1(), response.getPortNumber1());
            log.info("Gateway Secondary: {}:{}", response.getIpAddress2(), response.getPortNumber2());
        } else {
            session.transitionTo(OdpSessionState.ERROR);
            log.error("Lookup rejected: {}", response.getRejectReasonDescription());
        }

        // Complete the future if waiting
        if (lookupFuture != null) {
            lookupFuture.complete(response);
        }
    }

    /**
     * Set a future to be completed when lookup response is received
     */
    public void setLookupFuture(CompletableFuture<LookupResponse> future) {
        this.lookupFuture = future;
    }
}