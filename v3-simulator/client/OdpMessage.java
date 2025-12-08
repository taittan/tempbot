// src/main/java/com/odp/simulator/client/protocol/OdpMessage.java
package com.odp.simulator.client.protocol;

import io.netty.buffer.ByteBuf;

/**
 * Interface for all ODP messages
 */
public interface OdpMessage {

    /**
     * Get the message type
     */
    OdpMessageType getMessageType();

    /**
     * Get the message header
     */
    OdpMessageHeader getHeader();

    /**
     * Set the message header
     */
    void setHeader(OdpMessageHeader header);

    /**
     * Encode the message body to ByteBuf
     * Note: Header encoding is handled by the encoder
     */
    void encodeBody(ByteBuf buffer);

    /**
     * Decode the message body from ByteBuf
     * Note: Header decoding is handled by the decoder
     */
    void decodeBody(ByteBuf buffer, byte[] presenceMap);

    /**
     * Get the fields presence map for this message
     */
    byte[] getFieldsPresenceMap();

    /**
     * Calculate the body length in bytes
     */
    int calculateBodyLength();
}