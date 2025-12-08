// src/main/java/com/odp/simulator/client/handler/LogonResponseHandler.java
package com.odp.simulator.client.handler;

import com.odp.simulator.client.protocol.OdpMessage;
import com.odp.simulator.client.protocol.OdpMessageType;
import com.odp.simulator.client.protocol.messages.LogonResponse;
import com.odp.simulator.client.session.OdpSession;
import com.odp.simulator.client.session.OdpSessionManager;
import com.odp.simulator.client.session.OdpSessionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Handler for Logon Response messages
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogonResponseHandler implements OdpMessageHandler {

    private final OdpSessionManager sessionManager;
    
    // Used to signal completion of logon
    private volatile CompletableFuture<LogonResponse> logonFuture;

    @Override
    public OdpMessageType getMessageType() {
        return OdpMessageType.LOGON_RESPONSE;
    }

    @Override
    public void handle(OdpMessage message) {
        if (!(message instanceof LogonResponse response)) {
            log.error("Expected LogonResponse but got: {}", message.getClass().getSimpleName());
            return;
        }

        log.info("Received Logon Response - Status: {} ({})", 
                response.getSessionStatus(), response.getSessionStatusDescription());

        OdpSession session = sessionManager.getPrimarySession();

        if (response.isSessionActive()) {
            session.transitionTo(OdpSessionState.ACTIVE);
            session.setLogonTime(Instant.now());
            session.setTestMode(response.isTestMode());
            session.setExpectedIncomingSeqNum(response.getNextExpectedMsgSeqNum());

            log.info("Session active - Mode: {}, Next Expected SeqNum: {}", 
                    response.isTestMode() ? "Test" : "Production",
                    response.getNextExpectedMsgSeqNum());
            
            if (response.getLogonText() != null) {
                log.info("Logon message: {}", response.getLogonText());
            }
        } else {
            session.transitionTo(OdpSessionState.ERROR);
            log.error("Logon failed: {}", response.getSessionStatusDescription());
            
            if (response.getLogonText() != null) {
                log.error("Additional info: {}", response.getLogonText());
            }
        }

        // Complete the future if waiting
        if (logonFuture != null) {
            logonFuture.complete(response);
        }
    }

    /**
     * Set a future to be completed when logon response is received
     */
    public void setLogonFuture(CompletableFuture<LogonResponse> future) {
        this.logonFuture = future;
    }
}