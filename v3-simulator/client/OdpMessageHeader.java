// src/main/java/com/odp/simulator/client/protocol/OdpMessageHeader.java
package com.odp.simulator.client.protocol;

import lombok.Builder;
import lombok.Data;

/**
 * ODP Message Header as defined in Section 9.4
 */
@Data
@Builder
public class OdpMessageHeader {

    /**
     * Length of the message including all fields (header + body)
     * Data type: UInt16
     */
    private int length;

    /**
     * Message ID - defines the message type
     * Data type: UInt16
     */
    private int messageId;

    /**
     * Message sequence number
     * Data type: UInt64
     */
    private long msgSeqNum;

    /**
     * Comp ID assigned to the client
     * Data type: Char Array (11)
     */
    private String compId;

    /**
     * Message flags (8-bit field)
     * Bit 0 (0x01) = PossDup Flag
     * Bit 1 (0x02) = PossResend Flag
     * Data type: UInt8
     */
    private byte messageFlags;

    /**
     * Fields Presence Map - indicates which fields are present in the body
     * Data type: Bitmap (12)
     */
    private byte[] fieldsPresenceMap;

    /**
     * Check if PossDup flag is set
     */
    public boolean isPossDup() {
        return (messageFlags & 0x01) != 0;
    }

    /**
     * Check if PossResend flag is set
     */
    public boolean isPossResend() {
        return (messageFlags & 0x02) != 0;
    }

    /**
     * Set PossDup flag
     */
    public void setPossDup(boolean possDup) {
        if (possDup) {
            messageFlags |= 0x01;
        } else {
            messageFlags &= ~0x01;
        }
    }

    /**
     * Set PossResend flag
     */
    public void setPossResend(boolean possResend) {
        if (possResend) {
            messageFlags |= 0x02;
        } else {
            messageFlags &= ~0x02;
        }
    }
}