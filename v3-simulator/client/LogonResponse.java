// src/main/java/com/odp/simulator/client/protocol/messages/LogonResponse.java
package com.odp.simulator.client.protocol.messages;

import com.odp.simulator.client.protocol.OdpDataType;
import com.odp.simulator.client.protocol.OdpMessageType;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * Logon Response (6) - Section 9.6.2
 * 
 * The gateway sends this message to respond to a Logon Request.
 * 
 * Field Bit Positions:
 * - BP 0: Next Expected MsgSeqNum (Required)
 * - BP 1: Session Status (Required)
 * - BP 2: Test Message Indicator (Required)
 * - BP 3: Logon Text (Optional) - Present for certain Session Status values
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class LogonResponse extends BaseOdpMessage {

    // Bit positions in presence map
    private static final int BP_NEXT_EXPECTED_MSG_SEQ_NUM = 0;
    private static final int BP_SESSION_STATUS = 1;
    private static final int BP_TEST_MESSAGE_INDICATOR = 2;
    private static final int BP_LOGON_TEXT = 3;

    // Session Status values
    public static final int STATUS_SESSION_ACTIVE = 0;
    public static final int STATUS_PASSWORD_CHANGE = 1;
    public static final int STATUS_PASSWORD_DUE_TO_EXPIRE = 2;
    public static final int STATUS_NEW_PASSWORD_NOT_COMPLIANT = 3;
    public static final int STATUS_LOGOUT_COMPLETE = 4;
    public static final int STATUS_INVALID_USERNAME_OR_PASSWORD = 5;
    public static final int STATUS_ACCOUNT_LOCKED = 6;
    public static final int STATUS_LOGONS_NOT_ALLOWED = 7;
    public static final int STATUS_PASSWORD_EXPIRED = 8;
    public static final int STATUS_MSG_SEQ_NUM_TOO_LOW = 9;
    public static final int STATUS_NEXT_EXPECTED_MSG_SEQ_NUM_TOO_HIGH = 10;
    public static final int STATUS_PASSWORD_CHANGE_REQUIRED = 100;
    public static final int STATUS_OTHER = 101;

    // Test Message Indicator values
    public static final int TEST_MODE_NO = 0;  // Production Mode
    public static final int TEST_MODE_YES = 1; // Test Mode

    /**
     * Next Expected MsgSeqNum (UInt64)
     */
    private long nextExpectedMsgSeqNum;

    /**
     * Session Status (UInt8)
     */
    private int sessionStatus;

    /**
     * Test Message Indicator (UInt8)
     * 0 = Production Mode, 1 = Test Mode
     */
    private int testMessageIndicator;

    /**
     * Logon Text (Char Array 85) - Optional
     * Present for Session Status 2 (password due to expire) or 101 (other)
     */
    private String logonText;

    @Override
    public OdpMessageType getMessageType() {
        return OdpMessageType.LOGON_RESPONSE;
    }

    @Override
    public void encodeBody(ByteBuf buffer) {
        // Logon Response is an outbound message from gateway
        log.warn("LogonResponse.encodeBody() called - this is typically a gateway message");
    }

    @Override
    public void decodeBody(ByteBuf buffer, byte[] presenceMap) {
        log.debug("Decoding LogonResponse body, presenceMap: {}", bytesToHex(presenceMap));
        
        // BP 0: Next Expected MsgSeqNum (Required)
        if (isFieldPresent(presenceMap, BP_NEXT_EXPECTED_MSG_SEQ_NUM)) {
            this.nextExpectedMsgSeqNum = readUInt64LE(buffer);
            log.debug("Decoded nextExpectedMsgSeqNum: {}", nextExpectedMsgSeqNum);
        }

        // BP 1: Session Status (Required)
        if (isFieldPresent(presenceMap, BP_SESSION_STATUS)) {
            this.sessionStatus = readUInt8(buffer);
            log.debug("Decoded sessionStatus: {} ({})", sessionStatus, getSessionStatusDescription());
        }

        // BP 2: Test Message Indicator (Required)
        if (isFieldPresent(presenceMap, BP_TEST_MESSAGE_INDICATOR)) {
            this.testMessageIndicator = readUInt8(buffer);
            log.debug("Decoded testMessageIndicator: {} ({})", 
                    testMessageIndicator, isTestMode() ? "Test Mode" : "Production Mode");
        }

        // BP 3: Logon Text (Optional)
        if (isFieldPresent(presenceMap, BP_LOGON_TEXT)) {
            this.logonText = readCharArray(buffer, OdpDataType.LOGON_TEXT_SIZE);
            log.debug("Decoded logonText: {}", logonText);
        }
    }

    @Override
    public byte[] getFieldsPresenceMap() {
        byte[] presenceMap = createPresenceMap();
        
        setFieldPresent(presenceMap, BP_NEXT_EXPECTED_MSG_SEQ_NUM, true);
        setFieldPresent(presenceMap, BP_SESSION_STATUS, true);
        setFieldPresent(presenceMap, BP_TEST_MESSAGE_INDICATOR, true);
        setFieldPresent(presenceMap, BP_LOGON_TEXT, logonText != null && !logonText.isEmpty());
        
        return presenceMap;
    }

    @Override
    public int calculateBodyLength() {
        int length = 0;
        
        // Next Expected MsgSeqNum (UInt64)
        length += 8;
        
        // Session Status (UInt8)
        length += 1;
        
        // Test Message Indicator (UInt8)
        length += 1;
        
        // Logon Text (Optional)
        if (logonText != null && !logonText.isEmpty()) {
            length += OdpDataType.LOGON_TEXT_SIZE;
        }
        
        return length;
    }

    /**
     * Check if session is active
     */
    public boolean isSessionActive() {
        return sessionStatus == STATUS_SESSION_ACTIVE;
    }

    /**
     * Check if connected to test mode
     */
    public boolean isTestMode() {
        return testMessageIndicator == TEST_MODE_YES;
    }

    /**
     * Get session status description
     */
    public String getSessionStatusDescription() {
        return switch (sessionStatus) {
            case STATUS_SESSION_ACTIVE -> "Session active";
            case STATUS_PASSWORD_CHANGE -> "Session password change";
            case STATUS_PASSWORD_DUE_TO_EXPIRE -> "Session password due to expire";
            case STATUS_NEW_PASSWORD_NOT_COMPLIANT -> "New session password does not comply with policy";
            case STATUS_LOGOUT_COMPLETE -> "Session logout complete";
            case STATUS_INVALID_USERNAME_OR_PASSWORD -> "Invalid username or password";
            case STATUS_ACCOUNT_LOCKED -> "Account locked";
            case STATUS_LOGONS_NOT_ALLOWED -> "Logons are not allowed at this time";
            case STATUS_PASSWORD_EXPIRED -> "Password expired";
            case STATUS_MSG_SEQ_NUM_TOO_LOW -> "Received MsgSeqNum is too low";
            case STATUS_NEXT_EXPECTED_MSG_SEQ_NUM_TOO_HIGH -> "Received Next Expected MsgSeqNum is too high";
            case STATUS_PASSWORD_CHANGE_REQUIRED -> "Password change is required";
            case STATUS_OTHER -> "Other: " + (logonText != null ? logonText : "");
            default -> "Unknown status: " + sessionStatus;
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