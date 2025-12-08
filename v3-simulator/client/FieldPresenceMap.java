// src/main/java/com/odp/simulator/client/codec/FieldPresenceMap.java
package com.odp.simulator.client.codec;

import com.odp.simulator.client.protocol.OdpDataType;

/**
 * Utility class for working with Fields Presence Map
 * 
 * The Fields Presence Map is a 12-byte (96-bit) bitmap where each bit
 * represents whether a field is present in the message body.
 * 
 * Bit position 0 is the MSB of the first byte.
 */
public class FieldPresenceMap {

    private final byte[] bitmap;

    public FieldPresenceMap() {
        this.bitmap = new byte[OdpDataType.FIELDS_PRESENCE_MAP_SIZE];
    }

    public FieldPresenceMap(byte[] bitmap) {
        if (bitmap == null || bitmap.length != OdpDataType.FIELDS_PRESENCE_MAP_SIZE) {
            this.bitmap = new byte[OdpDataType.FIELDS_PRESENCE_MAP_SIZE];
            if (bitmap != null) {
                System.arraycopy(bitmap, 0, this.bitmap, 0, 
                        Math.min(bitmap.length, OdpDataType.FIELDS_PRESENCE_MAP_SIZE));
            }
        } else {
            this.bitmap = bitmap.clone();
        }
    }

    /**
     * Check if a field at the specified bit position is present
     * @param bitPosition Position starting from 0 (MSB of first byte)
     */
    public boolean isFieldPresent(int bitPosition) {
        if (bitPosition < 0 || bitPosition >= OdpDataType.FIELDS_PRESENCE_MAP_SIZE * 8) {
            return false;
        }
        
        int byteIndex = bitPosition / 8;
        int bitIndex = 7 - (bitPosition % 8); // MSB first
        
        return (bitmap[byteIndex] & (1 << bitIndex)) != 0;
    }

    /**
     * Set the presence of a field at the specified bit position
     * @param bitPosition Position starting from 0 (MSB of first byte)
     * @param present Whether the field is present
     */
    public void setFieldPresent(int bitPosition, boolean present) {
        if (bitPosition < 0 || bitPosition >= OdpDataType.FIELDS_PRESENCE_MAP_SIZE * 8) {
            return;
        }
        
        int byteIndex = bitPosition / 8;
        int bitIndex = 7 - (bitPosition % 8); // MSB first
        
        if (present) {
            bitmap[byteIndex] |= (1 << bitIndex);
        } else {
            bitmap[byteIndex] &= ~(1 << bitIndex);
        }
    }

    /**
     * Get the raw bitmap bytes
     */
    public byte[] getBytes() {
        return bitmap.clone();
    }

    /**
     * Clear all bits
     */
    public void clear() {
        for (int i = 0; i < bitmap.length; i++) {
            bitmap[i] = 0;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (byte b : bitmap) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}