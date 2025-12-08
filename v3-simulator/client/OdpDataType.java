// src/main/java/com/odp/simulator/client/protocol/OdpDataType.java
package com.odp.simulator.client.protocol;

/**
 * ODP Data Types as defined in Section 10 Data Dictionary
 * Note: Multi-byte integers use Little Endian encoding
 */
public final class OdpDataType {

    private OdpDataType() {
        // Utility class
    }

    // Null values for different data types
    public static final byte UINT8_NULL = (byte) 0xFF;           // 255
    public static final byte INT8_NULL = (byte) 0x80;            // -128
    public static final short UINT16_NULL = (short) 0xFFFF;      // 65535
    public static final short INT16_NULL = (short) 0x8000;       // -32768
    public static final int UINT32_NULL = 0xFFFFFFFF;            // 4294967295
    public static final int INT32_NULL = 0x80000000;             // -2147483648
    public static final long UINT64_NULL = 0xFFFFFFFFFFFFFFFFL;  // 18446744073709551615
    public static final long INT64_NULL = 0x8000000000000000L;   // -9223372036854775808

    // Field sizes in bytes
    public static final int COMP_ID_SIZE = 11;
    public static final int FIELDS_PRESENCE_MAP_SIZE = 12;
    public static final int PASSWORD_SIZE = 450;
    public static final int EP_APPL_VERSION_ID_SIZE = 24;
    public static final int LOGON_TEXT_SIZE = 85;
    public static final int IP_ADDRESS_SIZE = 15;

    // Header field sizes
    public static final int LENGTH_SIZE = 2;        // UInt16
    public static final int MESSAGE_ID_SIZE = 2;    // UInt16
    public static final int MSG_SEQ_NUM_SIZE = 8;   // UInt64
    public static final int MESSAGE_FLAGS_SIZE = 1; // UInt8

    // Total header size
    public static final int HEADER_SIZE = LENGTH_SIZE + MESSAGE_ID_SIZE + MSG_SEQ_NUM_SIZE 
            + COMP_ID_SIZE + MESSAGE_FLAGS_SIZE + FIELDS_PRESENCE_MAP_SIZE;
}