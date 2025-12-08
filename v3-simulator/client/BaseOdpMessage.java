// src/main/java/com/odp/simulator/client/protocol/messages/BaseOdpMessage.java
package com.odp.simulator.client.protocol.messages;

import com.odp.simulator.client.protocol.OdpDataType;
import com.odp.simulator.client.protocol.OdpMessage;
import com.odp.simulator.client.protocol.OdpMessageHeader;
import com.odp.simulator.client.protocol.OdpMessageType;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * Base class for all ODP messages providing common functionality
 */
@Slf4j
@Data
public abstract class BaseOdpMessage implements OdpMessage {

    protected OdpMessageHeader header;

    /**
     * Write a fixed-length char array to buffer
     * If value is shorter than length, pad with nulls
     * If value is longer, truncate
     */
    protected void writeCharArray(ByteBuf buffer, String value, int length) {
        byte[] bytes = new byte[length];
        if (value != null && !value.isEmpty()) {
            byte[] valueBytes = value.getBytes(StandardCharsets.US_ASCII);
            int copyLength = Math.min(valueBytes.length, length);
            System.arraycopy(valueBytes, 0, bytes, 0, copyLength);
            // If value occupies full length, no null terminator needed
            // If shorter, remaining bytes are already 0 (null)
        }
        buffer.writeBytes(bytes);
    }

    /**
     * Read a fixed-length char array from buffer
     * Returns the string value (null-terminated or full length)
     */
    protected String readCharArray(ByteBuf buffer, int length) {
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        
        // Find null terminator or use full length
        int strLen = 0;
        for (int i = 0; i < length; i++) {
            if (bytes[i] == 0) {
                break;
            }
            strLen++;
        }
        
        if (strLen == 0) {
            return null; // Empty field
        }
        
        return new String(bytes, 0, strLen, StandardCharsets.US_ASCII);
    }

    /**
     * Write UInt8 (1 byte unsigned)
     */
    protected void writeUInt8(ByteBuf buffer, int value) {
        buffer.writeByte(value & 0xFF);
    }

    /**
     * Read UInt8 (1 byte unsigned)
     */
    protected int readUInt8(ByteBuf buffer) {
        return buffer.readByte() & 0xFF;
    }

    /**
     * Write UInt16 Little Endian (2 bytes unsigned)
     */
    protected void writeUInt16LE(ByteBuf buffer, int value) {
        buffer.writeShortLE(value & 0xFFFF);
    }

    /**
     * Read UInt16 Little Endian (2 bytes unsigned)
     */
    protected int readUInt16LE(ByteBuf buffer) {
        return buffer.readShortLE() & 0xFFFF;
    }

    /**
     * Write UInt32 Little Endian (4 bytes unsigned)
     */
    protected void writeUInt32LE(ByteBuf buffer, long value) {
        buffer.writeIntLE((int) (value & 0xFFFFFFFFL));
    }

    /**
     * Read UInt32 Little Endian (4 bytes unsigned)
     */
    protected long readUInt32LE(ByteBuf buffer) {
        return buffer.readIntLE() & 0xFFFFFFFFL;
    }

    /**
     * Write UInt64 Little Endian (8 bytes unsigned)
     */
    protected void writeUInt64LE(ByteBuf buffer, long value) {
        buffer.writeLongLE(value);
    }

    /**
     * Read UInt64 Little Endian (8 bytes unsigned)
     */
    protected long readUInt64LE(ByteBuf buffer) {
        return buffer.readLongLE();
    }

    /**
     * Check if a specific bit is set in the presence map
     * Bit positions start from 0 (MSB of first byte)
     */
    protected boolean isFieldPresent(byte[] presenceMap, int bitPosition) {
        if (presenceMap == null || bitPosition < 0) {
            return false;
        }
        
        int byteIndex = bitPosition / 8;
        int bitIndex = 7 - (bitPosition % 8); // MSB first
        
        if (byteIndex >= presenceMap.length) {
            return false;
        }
        
        return (presenceMap[byteIndex] & (1 << bitIndex)) != 0;
    }

    /**
     * Set a specific bit in the presence map
     * Bit positions start from 0 (MSB of first byte)
     */
    protected void setFieldPresent(byte[] presenceMap, int bitPosition, boolean present) {
        if (presenceMap == null || bitPosition < 0) {
            return;
        }
        
        int byteIndex = bitPosition / 8;
        int bitIndex = 7 - (bitPosition % 8); // MSB first
        
        if (byteIndex >= presenceMap.length) {
            return;
        }
        
        if (present) {
            presenceMap[byteIndex] |= (1 << bitIndex);
        } else {
            presenceMap[byteIndex] &= ~(1 << bitIndex);
        }
    }

    /**
     * Create a new presence map with all bits cleared
     */
    protected byte[] createPresenceMap() {
        return new byte[OdpDataType.FIELDS_PRESENCE_MAP_SIZE];
    }

    @Override
    public abstract OdpMessageType getMessageType();

    @Override
    public abstract void encodeBody(ByteBuf buffer);

    @Override
    public abstract void decodeBody(ByteBuf buffer, byte[] presenceMap);

    @Override
    public abstract byte[] getFieldsPresenceMap();

    @Override
    public abstract int calculateBodyLength();
}