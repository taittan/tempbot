import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;

public class LookupRequestBuilder {

    /**
     * 构造 Lookup Request 消息
     * @param compId 客户端的 Comp ID（最多11个字符）
     * @return ByteBuf 包含完整的二进制消息
     */
    public static ByteBuf buildLookupRequest(String compId) {
        // Lookup Request 只有 Header，没有 Body
        int messageLength = OcgConstants.HEADER_SIZE; // 36 bytes
        
        ByteBuf buffer = Unpooled.buffer(messageLength);
        
        // 1. Length (UInt16) - 消息总长度
        buffer.writeShort(messageLength);
        
        // 2. Message ID (UInt16)
        buffer.writeShort(OcgConstants.MSG_ID_LOOKUP_REQUEST);
        
        // 3. MsgSeqNum (UInt64) - Lookup Request 必须设为 1
        buffer.writeLong(1L);
        
        // 4. Comp ID (Char Array 11) - 右边补空格或补0
        writeCompId(buffer, compId);
        
        // 5. Message Flags (UInt8) - 原始传输，设为 0
        buffer.writeByte(0);
        
        // 6. Fields Presence Map (Bitmap 12 bytes) - Lookup Request 无 body 字段，全部为 0
        buffer.writeBytes(new byte[OcgConstants.FIELDS_PRESENCE_MAP_SIZE]);
        
        return buffer;
    }
    
    /**
     * 写入 Comp ID，固定 11 字节，不足部分补 0x00
     */
    private static void writeCompId(ByteBuf buffer, String compId) {
        byte[] compIdBytes = compId.getBytes(StandardCharsets.US_ASCII);
        
        if (compIdBytes.length >= OcgConstants.COMP_ID_SIZE) {
            // 截断到 11 字节
            buffer.writeBytes(compIdBytes, 0, OcgConstants.COMP_ID_SIZE);
        } else {
            // 写入实际内容
            buffer.writeBytes(compIdBytes);
            // 补 0x00
            int padding = OcgConstants.COMP_ID_SIZE - compIdBytes.length;
            buffer.writeBytes(new byte[padding]);
        }
    }
}