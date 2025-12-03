package com.example.odp.builder;

import com.example.odp.constants.OdpConstants;
import com.example.odp.message.Heartbeat;
import com.example.odp.message.LookupRequest;
import com.example.odp.message.OdpHeader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class OdpMessageBuilder {
    
    /**
     * 构建 Lookup Request 消息
     */
    public static ByteBuf buildLookupRequest(ByteBufAllocator allocator, String compId) {
        LookupRequest request = LookupRequest.builder()
                .compId(compId)
                .build();
        
        OdpHeader header = request.buildHeader();
        ByteBuf buffer = allocator.buffer(header.getLength());
        
        writeHeader(buffer, header);
        
        log.debug("Built LookupRequest: compId={}, totalBytes={}", compId, buffer.readableBytes());
        logHexDump("LookupRequest", buffer);
        
        return buffer;
    }
    
    /**
     * 构建 Heartbeat 消息
     */
    public static ByteBuf buildHeartbeat(ByteBufAllocator allocator, String compId, long msgSeqNum) {
        Heartbeat heartbeat = Heartbeat.builder()
                .compId(compId)
                .msgSeqNum(msgSeqNum)
                .build();
        
        OdpHeader header = heartbeat.buildHeader();
        ByteBuf buffer = allocator.buffer(header.getLength());
        
        writeHeader(buffer, header);
        
        log.debug("Built Heartbeat: compId={}, seqNum={}, totalBytes={}", 
                compId, msgSeqNum, buffer.readableBytes());
        logHexDump("Heartbeat", buffer);
        
        return buffer;
    }
    
    /**
     * 写入消息头
     */
    private static void writeHeader(ByteBuf buffer, OdpHeader header) {
        // 根据字节序选择写入方法
        if (OdpConstants.BYTE_ORDER == java.nio.ByteOrder.LITTLE_ENDIAN) {
            // Little-Endian
            buffer.writeShortLE(header.getLength());
            buffer.writeShortLE(header.getMessageId());
            buffer.writeLongLE(header.getMsgSeqNum());
        } else {
            // Big-Endian
            buffer.writeShort(header.getLength());
            buffer.writeShort(header.getMessageId());
            buffer.writeLong(header.getMsgSeqNum());
        }
        
        // Comp ID (11 bytes)
        writeFixedString(buffer, header.getCompId(), OdpConstants.COMP_ID_SIZE);
        
        // Message Flags (1 byte)
        buffer.writeByte(header.getMessageFlags());
        
        // Fields Presence Map (12 bytes)
        byte[] presenceMap = header.getFieldsPresenceMap();
        if (presenceMap == null || presenceMap.length == 0) {
            buffer.writeBytes(new byte[OdpConstants.FIELDS_PRESENCE_MAP_SIZE]);
        } else {
            buffer.writeBytes(presenceMap);
            // 补齐到 12 bytes
            if (presenceMap.length < OdpConstants.FIELDS_PRESENCE_MAP_SIZE) {
                buffer.writeBytes(new byte[OdpConstants.FIELDS_PRESENCE_MAP_SIZE - presenceMap.length]);
            }
        }
    }
    
    /**
     * 写入固定长度字符串，不足部分补 0x00
     */
    private static void writeFixedString(ByteBuf buffer, String value, int fixedLength) {
        byte[] bytes = value != null ? value.getBytes(StandardCharsets.US_ASCII) : new byte[0];
        
        int writeLength = Math.min(bytes.length, fixedLength);
        buffer.writeBytes(bytes, 0, writeLength);
        
        // 补 0x00
        int padding = fixedLength - writeLength;
        if (padding > 0) {
            buffer.writeBytes(new byte[padding]);
        }
    }
    
    /**
     * 打印 Hex Dump（调试用）
     */
    private static void logHexDump(String messageName, ByteBuf buffer) {
        if (log.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append(messageName).append(" HexDump (").append(buffer.readableBytes()).append(" bytes):\n");
            
            int readerIndex = buffer.readerIndex();
            for (int i = 0; i < buffer.readableBytes(); i++) {
                if (i > 0 && i % 16 == 0) {
                    sb.append("\n");
                }
                sb.append(String.format("%02X ", buffer.getByte(readerIndex + i)));
            }
            log.trace(sb.toString());
        }
    }
}