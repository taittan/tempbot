// src/main/java/com/odp/simulator/client/session/OdpSession.java
package com.odp.simulator.client.session;

import io.netty.channel.Channel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents an ODP session with the gateway
 */
@Slf4j
@Data
public class OdpSession {

    private final String compId;
    private volatile OdpSessionState state = OdpSessionState.DISCONNECTED;
    private volatile Channel channel;
    
    // Gateway connection info from Lookup
    private String gatewayIpPrimary;
    private int gatewayPortPrimary;
    private String gatewayIpSecondary;
    private int gatewayPortSecondary;
    
    // Session parameters
    private int heartbeatIntervalSeconds;
    private boolean testMode;
    
    // Sequence numbers
    private final AtomicLong outgoingSeqNum = new AtomicLong(1);
    private final AtomicLong expectedIncomingSeqNum = new AtomicLong(1);
    
    // Timestamps
    private Instant lastSentTime;
    private Instant lastReceivedTime;
    private Instant logonTime;

    public OdpSession(String compId) {
        this.compId = compId;
    }

    /**
     * Get and increment the outgoing sequence number
     */
    public long getNextOutgoingSeqNum() {
        return outgoingSeqNum.getAndIncrement();
    }

    /**
     * Get the current outgoing sequence number without incrementing
     */
    public long getCurrentOutgoingSeqNum() {
        return outgoingSeqNum.get();
    }

    /**
     * Reset the outgoing sequence number
     */
    public void resetOutgoingSeqNum(long value) {
        outgoingSeqNum.set(value);
    }

    /**
     * Get the expected incoming sequence number
     */
    public long getExpectedIncomingSeqNum() {
        return expectedIncomingSeqNum.get();
    }

    /**
     * Set the expected incoming sequence number
     */
    public void setExpectedIncomingSeqNum(long value) {
        expectedIncomingSeqNum.set(value);
    }

    /**
     * Increment the expected incoming sequence number
     */
    public void incrementExpectedIncomingSeqNum() {
        expectedIncomingSeqNum.incrementAndGet();
    }

    /**
     * Check if the session is active
     */
    public boolean isActive() {
        return state == OdpSessionState.ACTIVE && channel != null && channel.isActive();
    }

    /**
     * Check if the session is connected (channel exists and is active)
     */
    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    /**
     * Update the last sent timestamp
     */
    public void updateLastSentTime() {
        this.lastSentTime = Instant.now();
    }

    /**
     * Update the last received timestamp
     */
    public void updateLastReceivedTime() {
        this.lastReceivedTime = Instant.now();
    }

    /**
     * Transition to a new state
     */
    public void transitionTo(OdpSessionState newState) {
        OdpSessionState oldState = this.state;
        this.state = newState;
        log.info("Session {} state transition: {} -> {}", compId, oldState, newState);
    }
}