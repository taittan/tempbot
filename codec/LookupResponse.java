import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

public class LookupResponseParser {

    /**
     * Lookup Response 解析结果
     */
    public static class LookupResponse {
        // Header 字段
        public int length;
        public int messageId;
        public long msgSeqNum;
        public String compId;
        public int messageFlags;
        public byte[] fieldsPresenceMap;
        
        // Body 字段
        public int lookupStatus;         // 0=Accepted, 1=Rejected
        public int lookupRejectReason;   // 0=Invalid CompID/IP, 1=Blocked
        public String ipAddress1;
        public int portNumber1;
        public String ipAddress2;
        public int portNumber2;
        
        public boolean isAccepted() {
            return lookupStatus == 0;
        }
        
        @Override
        public String toString() {
            if (isAccepted()) {
                return String.format("LookupResponse[Accepted, IP1=%s:%d, IP2=%s:%d]",
                        ipAddress1, portNumber1, ipAddress2, portNumber2);
            } else {
                return String.format("LookupResponse[Rejected, Reason=%d]", lookupRejectReason);
            }
        }
    }

    /**
     * 解析 Lookup Response 消息
     * @param buffer 收到的二进制数据
     * @return 解析后的 LookupResponse 对象
     */
    public static LookupResponse parse(ByteBuf buffer) {
        LookupResponse response = new LookupResponse();
        
        // ========== 解析 Header ==========
        
        // 1. Length (UInt16)
        response.length = buffer.readUnsignedShort();
        
        // 2. Message ID (UInt16)
        response.messageId = buffer.readUnsignedShort();
        
        // 3. MsgSeqNum (UInt64)
        response.msgSeqNum = buffer.readLong();
        
        // 4. Comp ID (Char Array 11)
        response.compId = readCompId(buffer);
        
        // 5. Message Flags (UInt8)
        response.messageFlags = buffer.readUnsignedByte();
        
        // 6. Fields Presence Map (Bitmap 12 bytes)
        response.fieldsPresenceMap = new byte[OcgConstants.FIELDS_PRESENCE_MAP_SIZE];
        buffer.readBytes(response.fieldsPresenceMap);
        
        // ========== 解析 Body（根据 Fields Presence Map）==========
        
        // BP 0: Lookup Status (必须存在)
        if (isBitSet(response.fieldsPresenceMap, 0)) {
            response.lookupStatus = buffer.readUnsignedByte();
        }
        
        // BP 1: Lookup Reject Reason (必须存在)
        if (isBitSet(response.fieldsPresenceMap, 1)) {
            response.lookupRejectReason = buffer.readUnsignedShort();
        }
        
        // BP 2: IP Address 1 (仅当 Accepted)
        if (isBitSet(response.fieldsPresenceMap, 2)) {
            response.ipAddress1 = readIpAddress(buffer);
        }
        
        // BP 3: Port Number 1 (仅当 Accepted)
        if (isBitSet(response.fieldsPresenceMap, 3)) {
            response.portNumber1 = buffer.readUnsignedShort();
        }
        
        // BP 4: IP Address 2 (仅当 Accepted)
        if (isBitSet(response.fieldsPresenceMap, 4)) {
            response.ipAddress2 = readIpAddress(buffer);
        }
        
        // BP 5: Port Number 2 (仅当 Accepted)
        if (isBitSet(response.fieldsPresenceMap, 5)) {
            response.portNumber2 = buffer.readUnsignedShort();
        }
        
        return response;
    }
    
    /**
     * 检查 Fields Presence Map 中指定 bit position 是否为 1
     * Bit 0 是最高位（MSB）
     */
    private static boolean isBitSet(byte[] presenceMap, int bitPosition) {
        int byteIndex = bitPosition / 8;
        int bitIndex = 7 - (bitPosition % 8);  // MSB 优先
        
        if (byteIndex >= presenceMap.length) {
            return false;
        }
        
        return ((presenceMap[byteIndex] >> bitIndex) & 1) == 1;
    }
    
    /**
     * 读取 Comp ID (11 bytes)，去除尾部的 0x00 或空格
     */
    private static String readCompId(ByteBuf buffer) {
        byte[] bytes = new byte[OcgConstants.COMP_ID_SIZE];
        buffer.readBytes(bytes);
        return trimNullAndSpace(bytes);
    }
    
    /**
     * 读取 IP Address (15 bytes)，去除尾部的 0x00 或空格
     */
    private static String readIpAddress(ByteBuf buffer) {
        byte[] bytes = new byte[OcgConstants.IP_ADDRESS_SIZE];
        buffer.readBytes(bytes);
        return trimNullAndSpace(bytes);
    }
    
    /**
     * 去除字节数组尾部的 0x00 和空格，转为字符串
     */
    private static String trimNullAndSpace(byte[] bytes) {
        int length = bytes.length;
        while (length > 0 && (bytes[length - 1] == 0x00 || bytes[length - 1] == 0x20)) {
            length--;
        }
        return new String(bytes, 0, length, StandardCharsets.US_ASCII);
    }
}