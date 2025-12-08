// src/main/java/com/odp/simulator/client/protocol/messages/HeartbeatMessage.java
package com.odp.simulator.client.protocol.messages;

import com.odp.simulator.client.protocol.OdpMessageType;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * Heartbeat (0) - Section 9.3
 * 
 * Used to exercise the communication line during periods of inactivity
 * and verify that the interfaces at each end are available.
 * 
 * The heartbeat message has no body fields.
 * Both client and gateway can send this message.
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class HeartbeatMessage extends BaseOdpMessage {

    @Override
    public OdpMessageType getMessageType() {
        return OdpMessageType.HEARTBEAT;
    }

    @Override
    public void encodeBody(ByteBuf buffer) {
        // No body fields for Heartbeat
        log.trace("Encoding Heartbeat body (empty)");
    }

    @Override
    public void decodeBody(ByteBuf buffer, byte[] presenceMap) {
        // No body fields for Heartbeat
        log.trace("Decoding Heartbeat body (empty)");
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