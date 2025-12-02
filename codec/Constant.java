public class OcgConstants {
    // Message IDs
    public static final int MSG_ID_LOOKUP_REQUEST = 9;
    public static final int MSG_ID_LOOKUP_RESPONSE = 10;
    
    // Header 各字段长度
    public static final int LENGTH_SIZE = 2;          // UInt16
    public static final int MESSAGE_ID_SIZE = 2;      // UInt16
    public static final int MSG_SEQ_NUM_SIZE = 8;     // UInt64
    public static final int COMP_ID_SIZE = 11;        // Char Array(11)
    public static final int MESSAGE_FLAGS_SIZE = 1;   // UInt8
    public static final int FIELDS_PRESENCE_MAP_SIZE = 12; // Bitmap(12)
    
    // Header 总长度
    public static final int HEADER_SIZE = LENGTH_SIZE + MESSAGE_ID_SIZE + MSG_SEQ_NUM_SIZE 
                                        + COMP_ID_SIZE + MESSAGE_FLAGS_SIZE + FIELDS_PRESENCE_MAP_SIZE;
    // = 2 + 2 + 8 + 11 + 1 + 12 = 36
    
    // Lookup Response Body 字段长度
    public static final int LOOKUP_STATUS_SIZE = 1;        // UInt8
    public static final int LOOKUP_REJECT_REASON_SIZE = 2; // UInt16
    public static final int IP_ADDRESS_SIZE = 15;          // Char Array(15)
    public static final int PORT_NUMBER_SIZE = 2;          // UInt16
}