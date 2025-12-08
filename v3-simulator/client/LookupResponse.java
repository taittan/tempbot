// src/main/java/com/odp/simulator/client/protocol/messages/LookupResponse.java
package com.odp.simulator.client.protocol.messages;

import com.odp.simulator.client.protocol.OdpDataType;
import com.odp.simulator.client.protocol.OdpMessageType;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * Lookup Response (10) - Section 9.5.2
 * 
 * The gateway sends this message in response to a Lookup Request.
 * 
 * Field Bit Positions:
 * - BP 0: Lookup Status (Required)
 * - BP 1: Lookup Reject Reason (Required when status = Rejected)
 * - BP 2: IP Address 1 (Primary, present when status = Accepted)
 * - BP 3: Port Number 1 (present when status = Accepted)
 * - BP 4: IP Address 2 (Secondary, present when status = Accepted)
 * - BP 5: Port Number 2 (present when status = Accepted)
 * 
 * Note: MsgSeqNum in header is always set to 1.
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class LookupResponse extends BaseOdpMessage {

    // Bit positions in presence map
    private static final int BP_LOOKUP_STATUS = 0;
    private static final int BP_LOOKUP_REJECT_REASON = 1;
    private static final int BP_IP_ADDRESS_1 = 2;
    private static final int BP_PORT_NUMBER_1 = 3;
    private static final int BP_IP_ADDRESS_2 = 4;
    private static final int BP_PORT_NUMBER_2 = 5;

    // Lookup Status values
    public static final int STATUS_ACCEPTED = 0;
    public static final int STATUS_REJECTED = 1;

    // Reject Reason values
    public static final int REJECT_INVALID_COMP_ID_OR_IP = 0;
    public static final int REJECT_CLIENT_BLOCKED = 1;

    /**
     * Lookup Status (UInt8)
     * 0 = Accepted, 1 = Rejected
     */
    private int lookupStatus;

    /**
     * Lookup Reject Reason (UInt16)
     * Present when lookupStatus = 1 (Rejected)
     * 0 = Invalid CompID or invalid IP address
     * 1 = Client (Comp ID) is blocked
     */
    private int lookupRejectReason;

    /**
     * IP Address 1 - Primary (Char Array 15)
     * Present when lookupStatus = 0 (Accepted)
     */
    private String ipAddress1;

    /**
     * Port Number 1 (UInt16)
     * Present when lookupStatus = 0 (Accepted)
     */
    private int portNumber1;

    /**
     * IP Address 2 - Secondary (Char Array 15)
     * Present when lookupStatus = 0 (Accepted)
     */
    private String ipAddress2;

    /**
     * Port Number 2 (UInt16)
     * Present when lookupStatus = 0 (Accepted)
     */
    private int portNumber2;

    @Override
    public OdpMessageType getMessageType() {
        return OdpMessageType.LOOKUP_RESPONSE;
    }

    @Override
    public void encodeBody(ByteBuf buffer) {
        // Lookup Response is an outbound message from gateway
        // Client typically doesn't encode this message
        log.warn("LookupResponse.encodeBody() called - this is typically a gateway message");
    }

    @Override
    public void decodeBody(ByteBuf buffer, byte[] presenceMap) {
        log.debug("Decoding LookupResponse body, presenceMap: {}", bytesToHex(presenceMap));
        
        // BP 0: Lookup Status (Required)
        if (isFieldPresent(presenceMap, BP_LOOKUP_STATUS)) {
            this.lookupStatus = readUInt8(buffer);
            log.debug("Decoded lookupStatus: {}", lookupStatus);
        }

        // BP 1: Lookup Reject Reason
        if (isFieldPresent(presenceMap, BP_LOOKUP_REJECT_REASON)) {
            this.lookupRejectReason = readUInt16LE(buffer);
            log.debug("Decoded lookupRejectReason: {}", lookupRejectReason);
        }

        // BP 2: IP Address 1 (Primary)
        if (isFieldPresent(presenceMap, BP_IP_ADDRESS_1)) {
            this.ipAddress1 = readCharArray(buffer, OdpDataType.IP_ADDRESS_SIZE);
            log.debug("Decoded ipAddress1: {}", ipAddress1);
        }

        // BP 3: Port Number 1
        if (isFieldPresent(presenceMap, BP_PORT_NUMBER_1)) {
            this.portNumber1 = readUInt16LE(buffer);
            log.debug("Decoded portNumber1: {}", portNumber1);
        }

        // BP 4: IP Address 2 (Secondary)
        if (isFieldPresent(presenceMap, BP_IP_ADDRESS_2)) {
            this.ipAddress2 = readCharArray(buffer, OdpDataType.IP_ADDRESS_SIZE);
            log.debug("Decoded ipAddress2: {}", ipAddress2);
        }

        // BP 5: Port Number 2
        if (isFieldPresent(presenceMap, BP_PORT_NUMBER_2)) {
            this.portNumber2 = readUInt16LE(buffer);
            log.debug("Decoded portNumber2: {}", portNumber2);
        }
    }

    @Override
    public byte[] getFieldsPresenceMap() {
        byte[] presenceMap = createPresenceMap();
        
        setFieldPresent(presenceMap, BP_LOOKUP_STATUS, true);
        
        if (lookupStatus == STATUS_REJECTED) {
            setFieldPresent(presenceMap, BP_LOOKUP_REJECT_REASON, true);
        } else if (lookupStatus == STATUS_ACCEPTED) {
            setFieldPresent(presenceMap, BP_IP_ADDRESS_1, ipAddress1 != null);
            setFieldPresent(presenceMap, BP_PORT_NUMBER_1, true);
            setFieldPresent(presenceMap, BP_IP_ADDRESS_2, ipAddress2 != null);
            setFieldPresent(presenceMap, BP_PORT_NUMBER_2, true);
        }
        
        return presenceMap;
    }

    @Override
    public int calculateBodyLength() {
        int length = 1; // Lookup Status (UInt8)
        
        if (lookupStatus == STATUS_REJECTED) {
            length += 2; // Lookup Reject Reason (UInt16)
        } else if (lookupStatus == STATUS_ACCEPTED) {
            length += OdpDataType.IP_ADDRESS_SIZE; // IP Address 1
            length += 2; // Port Number 1 (UInt16)
            length += OdpDataType.IP_ADDRESS_SIZE; // IP Address 2
            length += 2; // Port Number 2 (UInt16)
        }
        
        return length;
    }

    /**
     * Check if the lookup was accepted
     */
    public boolean isAccepted() {
        return lookupStatus == STATUS_ACCEPTED;
    }

    /**
     * Get reject reason description
     */
    public String getRejectReasonDescription() {
        return switch (lookupRejectReason) {
            case REJECT_INVALID_COMP_ID_OR_IP -> "Invalid CompID or invalid IP address";
            case REJECT_CLIENT_BLOCKED -> "Client (Comp ID) is blocked";
            default -> "Unknown reject reason: " + lookupRejectReason;
        };
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}