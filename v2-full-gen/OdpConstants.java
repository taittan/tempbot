package com.example.odp.constants;

import java.nio.ByteOrder;

public final class OdpConstants {
    
    private OdpConstants() {}
    
    // ==================== 字节序配置 ====================
    // 根据 ODP 协议文档，确认字节序（通常港交所使用 Little-Endian）
    public static final ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
    
    // ==================== Message IDs ====================
    public static final int MSG_ID_HEARTBEAT = 0;
    public static final int MSG_ID_TEST_REQUEST = 1;
    public static final int MSG_ID_RESEND_REQUEST = 2;
    public static final int MSG_ID_REJECT = 3;
    public static final int MSG_ID_SEQUENCE_RESET = 4;
    public static final int MSG_ID_LOGON_REQUEST = 5;
    public static final int MSG_ID_LOGON_RESPONSE = 6;
    public static final int MSG_ID_LOGOUT = 7;
    public static final int MSG_ID_BUSINESS_MESSAGE_REJECT = 8;
    public static final int MSG_ID_LOOKUP_REQUEST = 9;
    public static final int MSG_ID_LOOKUP_RESPONSE = 10;
    public static final int MSG_ID_THROTTLE_ENTITLEMENT_REQUEST = 11;
    public static final int MSG_ID_THROTTLE_ENTITLEMENT_RESPONSE = 12;
    
    // ==================== Header 字段长度 ====================
    public static final int LENGTH_SIZE = 2;               // UInt16
    public static final int MESSAGE_ID_SIZE = 2;           // UInt16
    public static final int MSG_SEQ_NUM_SIZE = 8;          // UInt64
    public static final int COMP_ID_SIZE = 11;             // Char Array(11)
    public static final int MESSAGE_FLAGS_SIZE = 1;        // UInt8
    public static final int FIELDS_PRESENCE_MAP_SIZE = 12; // Bitmap(12) = 96 bits
    
    // Header 总长度 = 2 + 2 + 8 + 11 + 1 + 12 = 36 bytes
    public static final int HEADER_SIZE = LENGTH_SIZE + MESSAGE_ID_SIZE + MSG_SEQ_NUM_SIZE 
                                        + COMP_ID_SIZE + MESSAGE_FLAGS_SIZE + FIELDS_PRESENCE_MAP_SIZE;
    
    // ==================== Lookup Response Body 字段长度 ====================
    public static final int LOOKUP_STATUS_SIZE = 1;        // UInt8
    public static final int LOOKUP_REJECT_REASON_SIZE = 2; // UInt16
    public static final int IP_ADDRESS_SIZE = 15;          // Char Array(15)
    public static final int PORT_NUMBER_SIZE = 2;          // UInt16
    
    // ==================== Message Flags ====================
    public static final int FLAG_POSS_DUP = 0x01;          // Bit 0
    public static final int FLAG_POSS_RESEND = 0x02;       // Bit 1
    
    // ==================== Lookup Status ====================
    public static final int LOOKUP_STATUS_ACCEPTED = 0;
    public static final int LOOKUP_STATUS_REJECTED = 1;
    
    // ==================== Lookup Reject Reason ====================
    public static final int LOOKUP_REJECT_INVALID_COMPID = 0;
    public static final int LOOKUP_REJECT_BLOCKED = 1;
}