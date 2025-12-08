// src/main/java/com/odp/simulator/client/protocol/OdpMessageFactory.java
package com.odp.simulator.client.protocol;

import com.odp.simulator.client.protocol.messages.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory for creating ODP message instances based on message type
 */
@Slf4j
public class OdpMessageFactory {

    private OdpMessageFactory() {
        // Utility class
    }

    /**
     * Create a message instance based on message type
     */
    public static OdpMessage createMessage(OdpMessageType messageType) {
        return switch (messageType) {
            case HEARTBEAT -> new HeartbeatMessage();
            case LOOKUP_REQUEST -> new LookupRequest();
            case LOOKUP_RESPONSE -> new LookupResponse();
            case LOGON_REQUEST -> new LogonRequest();
            case LOGON_RESPONSE -> new LogonResponse();
            // TODO: Add more message types as needed
            // case NEW_ORDER_SINGLE -> new NewOrderSingle();
            // case ORDER_ACCEPTED -> new OrderAccepted();
            // case ORDER_REJECTED -> new OrderRejected();
            default -> {
                log.warn("Unsupported message type: {}", messageType);
                yield null;
            }
        };
    }

    /**
     * Create a message instance based on message ID
     */
    public static OdpMessage createMessage(int messageId) {
        OdpMessageType messageType = OdpMessageType.fromMessageId(messageId);
        return createMessage(messageType);
    }
}