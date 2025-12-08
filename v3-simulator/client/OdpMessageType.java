// src/main/java/com/odp/simulator/client/protocol/OdpMessageType.java
package com.odp.simulator.client.protocol;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * ODP Message Types as defined in Section 9.3
 */
@Getter
@RequiredArgsConstructor
public enum OdpMessageType {

    // Administrative Messages
    HEARTBEAT(0, "Heartbeat"),
    TEST_REQUEST(1, "Test Request"),
    RESEND_REQUEST(2, "Resend Request"),
    REJECT(3, "Reject"),
    SEQUENCE_RESET(4, "Sequence Reset"),
    LOGON_REQUEST(5, "Logon Request"),
    LOGON_RESPONSE(6, "Logon Response"),
    LOGOUT(7, "Logout"),
    BUSINESS_MESSAGE_REJECT(8, "Business Message Reject"),
    LOOKUP_REQUEST(9, "Lookup Request"),
    LOOKUP_RESPONSE(10, "Lookup Response"),
    THROTTLE_ENTITLEMENT_REQUEST(11, "Throttle Entitlement Request"),
    THROTTLE_ENTITLEMENT_RESPONSE(12, "Throttle Entitlement Response"),

    // Order Messages
    NEW_ORDER_SINGLE(21, "New Order Single"),
    ORDER_AMEND_REQUEST(22, "Order Amend Request"),
    ORDER_CANCEL_REQUEST(23, "Order Cancel Request"),
    ORDER_REJECTED(24, "Order Rejected"),
    ORDER_AMEND_REJECTED(25, "Order Amend Rejected"),
    ORDER_CANCEL_REJECTED(26, "Order Cancel Rejected"),
    ORDER_ACCEPTED(27, "Order Accepted"),

    // Quote Messages
    MASS_QUOTE(51, "Mass Quote"),
    SINGLE_QUOTE(53, "Single Quote"),
    QUOTE_REQUEST(71, "Quote Request"),

    // Unknown message type
    UNKNOWN(-1, "Unknown");

    private final int messageId;
    private final String description;

    private static final Map<Integer, OdpMessageType> ID_MAP = new HashMap<>();

    static {
        for (OdpMessageType type : values()) {
            ID_MAP.put(type.messageId, type);
        }
    }

    public static OdpMessageType fromMessageId(int messageId) {
        return ID_MAP.getOrDefault(messageId, UNKNOWN);
    }
}