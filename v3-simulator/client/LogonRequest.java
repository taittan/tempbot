// src/main/java/com/odp/simulator/client/protocol/messages/LogonRequest.java
package com.odp.simulator.client.protocol.messages;

import com.odp.simulator.client.protocol.OdpDataType;
import com.odp.simulator.client.protocol.OdpMessageType;
import io.netty.buffer.ByteBuf;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Logon Request (5) - Section 9.6.1
 * 
 * The client sends this message to the gateway to initiate a session.
 * 
 * Field Bit Positions:
 * - BP 0: Password (Required) - Encrypted password with login time prefix
 * - BP 1: Heartbeat Interval (Required) - 1-60 seconds
 * - BP 2: Next Expected MsgSeqNum (Required)
 * - BP 3: EP Appl Version ID (Required) - BSS version certified by HKEX
 * - BP 4: New Password (Optional) - For password change
 * 
 * Password Encryption (Section 4.4 & 4.5):
 * 1. Prefix login time in UTC (YYYYMMDDHHMMSS) to password
 * 2. Encrypt using 2048-bit RSA with OAEP padding
 * 3. Encode result in Base64
 */
@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class LogonRequest extends BaseOdpMessage {

    // Bit positions in presence map
    private static final int BP_PASSWORD = 0;
    private static final int BP_HEARTBEAT_INTERVAL = 1;
    private static final int BP_NEXT_EXPECTED_MSG_SEQ_NUM = 2;
    private static final int BP_EP_APPL_VERSION_ID = 3;
    private static final int BP_NEW_PASSWORD = 4;

    /**
     * Encrypted password (Char Array 450)
     * Must be encrypted with RSA-OAEP and Base64 encoded
     * Password should be prefixed with login time (YYYYMMDDHHMMSS)
     */
    private String password;

    /**
     * Heartbeat interval in seconds (UInt32)
     * Valid range: 1-60
     */
    private long heartbeatInterval;

    /**
     * Next expected message sequence number (UInt64)
     */
    private long nextExpectedMsgSeqNum;

    /**
     * EP Application Version ID (Char Array 24)
     * BSS version certified by HKEX
     */
    private String epApplVersionId;

    /**
     * New encrypted password (Char Array 450) - Optional
     * For password change
     */
    private String newPassword;

    @Override
    public OdpMessageType getMessageType() {
        return OdpMessageType.LOGON_REQUEST;
    }

    @Override
    public void encodeBody(ByteBuf buffer) {
        log.debug("Encoding LogonRequest body");
        
        byte[] presenceMap = getFieldsPresenceMap();
        
        // BP 0: Password (Required)
        if (isFieldPresent(presenceMap, BP_PASSWORD)) {
            writeCharArray(buffer, password, OdpDataType.PASSWORD_SIZE);
            log.debug("Encoded password (length: {})", password != null ? password.length() : 0);
        }

        // BP 1: Heartbeat Interval (Required)
        if (isFieldPresent(presenceMap, BP_HEARTBEAT_INTERVAL)) {
            writeUInt32LE(buffer, heartbeatInterval);
            log.debug("Encoded heartbeatInterval: {}", heartbeatInterval);
        }

        // BP 2: Next Expected MsgSeqNum (Required)
        if (isFieldPresent(presenceMap, BP_NEXT_EXPECTED_MSG_SEQ_NUM)) {
            writeUInt64LE(buffer, nextExpectedMsgSeqNum);
            log.debug("Encoded nextExpectedMsgSeqNum: {}", nextExpectedMsgSeqNum);
        }

        // BP 3: EP Appl Version ID (Required)
        if (isFieldPresent(presenceMap, BP_EP_APPL_VERSION_ID)) {
            writeCharArray(buffer, epApplVersionId, OdpDataType.EP_APPL_VERSION_ID_SIZE);
            log.debug("Encoded epApplVersionId: {}", epApplVersionId);
        }

        // BP 4: New Password (Optional)
        if (isFieldPresent(presenceMap, BP_NEW_PASSWORD)) {
            writeCharArray(buffer, newPassword, OdpDataType.PASSWORD_SIZE);
            log.debug("Encoded newPassword");
        }
    }

    @Override
    public void decodeBody(ByteBuf buffer, byte[] presenceMap) {
        // Logon Request is an inbound message from client
        // Client typically doesn't decode its own request
        log.warn("LogonRequest.decodeBody() called - this is typically a client message");
    }

    @Override
    public byte[] getFieldsPresenceMap() {
        byte[] presenceMap = createPresenceMap();
        
        // Required fields
        setFieldPresent(presenceMap, BP_PASSWORD, true);
        setFieldPresent(presenceMap, BP_HEARTBEAT_INTERVAL, true);
        setFieldPresent(presenceMap, BP_NEXT_EXPECTED_MSG_SEQ_NUM, true);
        setFieldPresent(presenceMap, BP_EP_APPL_VERSION_ID, true);
        
        // Optional field
        setFieldPresent(presenceMap, BP_NEW_PASSWORD, newPassword != null && !newPassword.isEmpty());
        
        return presenceMap;
    }

    @Override
    public int calculateBodyLength() {
        int length = 0;
        
        // Password (Char Array 450)
        length += OdpDataType.PASSWORD_SIZE;
        
        // Heartbeat Interval (UInt32)
        length += 4;
        
        // Next Expected MsgSeqNum (UInt64)
        length += 8;
        
        // EP Appl Version ID (Char Array 24)
        length += OdpDataType.EP_APPL_VERSION_ID_SIZE;
        
        // New Password (Optional)
        if (newPassword != null && !newPassword.isEmpty()) {
            length += OdpDataType.PASSWORD_SIZE;
        }
        
        return length;
    }
}