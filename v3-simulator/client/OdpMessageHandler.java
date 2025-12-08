// src/main/java/com/odp/simulator/client/handler/OdpMessageHandler.java
package com.odp.simulator.client.handler;

import com.odp.simulator.client.protocol.OdpMessage;
import com.odp.simulator.client.protocol.OdpMessageType;

/**
 * Interface for handling specific ODP message types
 */
public interface OdpMessageHandler {

    /**
     * Get the message type this handler processes
     */
    OdpMessageType getMessageType();

    /**
     * Handle the message
     */
    void handle(OdpMessage message);
}