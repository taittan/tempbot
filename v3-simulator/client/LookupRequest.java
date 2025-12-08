// src/main/java/com/odp/simulator/client/protocol/messages/LookupRequest.java
package com.odp.simulator.client.protocol.messages;

import com.odp.simulator.client.protocol.OdpMessageType;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * Lookup Request (9) - Section 9.5.1
 * 
 * The client sends this message to the Lookup Service to get the 
 * gateway connection point (IP/Port pair) for the Comp ID.
 * 
 * Note: This message has no body fields. The Comp ID in the header
 * is used to determine the connection point.
 * 
 * Important: MsgSeqNum in header must be set to 1.
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class LookupRequest extends BaseOdpMessage {

    @Override
    public OdpMessageType getMessageType() {
        return OdpMessageType.LOOKUP_REQUEST;
    }

    @Override
    public void encodeBody(ByteBuf buffer) {
        // No body fields for Lookup Request
        log.debug("Encoding LookupRequest body (empty)");
    }

    @Override
    public void decodeBody(ByteBuf buffer, byte[] presenceMap) {
        // No body fields for Lookup Request
        log.debug("Decoding LookupRequest body (empty)");
    }

    @Override
    public byte[] getFieldsPresenceMap() {
        // All bits are 0 since there are no fields
        return createPresenceMap();
    }

    @Override
    public int calculateBodyLength() {
        // No body fields
        return 0;
    }
}