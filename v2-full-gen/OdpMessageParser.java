package com.example.odp.parser;

import com.example.odp.constants.OdpConstants;
import com.example.odp.message.LookupResponse;
import com.example.odp.message.OdpHeader;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class OdpMessageParser {
    
    /**
     * 解析消息头
     */
    public static OdpHeader parseHeader(ByteBuf buffer) {
        OdpHeader header = new OdpHeader();
        
        if (OdpConstants.BYTE_ORDER == java.nio.ByteOrder.LITTLE_ENDIAN) {
            header.setLength(buffer.readUnsignedShortLE());
            header.setMessageId(buffer.readUnsignedShortLE());
            header.setMsgSeqNum(buffer.readLongLE());
        } else {
            header.setLength(buffer.readUnsignedShort());
            header.setMessageId(buffer.readUnsignedShort());
            header.setMsgSeqNum(buffer.readLong());
        }
        
        header.setCompId(readFixedString(buffer, OdpConstants.COMP_ID_SIZE));
        header.setMessageFlags(buffer.readUnsignedByte());
        
        byte[] presenceMap = new byte[OdpConstants.FIELDS_PRESENCE_MAP_SIZE];
        buffer.readBytes(presenceMap);
        header.setFieldsPresenceMap(presenceMap);
        
        log.debug("Parsed header: {}", header);
        logPresenceMap(presenceMap);
        
        return header;
    }
    
    /**
     * 解析 Lookup Response
     */
    public static LookupResponse parseLookupResponse(ByteBuf buffer) {
        log.debug("Parsing LookupResponse, readable bytes: {}", buffer.readableBytes());
        logHexDump("LookupResponse Raw", buffer);
        
        // 解析 Header
        OdpHeader header = parseHeader(buffer);
        
        LookupResponse response = new LookupResponse();
        response.setHeader(header);
        
        byte[] presenceMap = header.getFieldsPresenceMap();
        
        // BP 0: Lookup Status (Required)
        if (isBitSet(presenceMap, 0)) {
            response.setLookupStatus(buffer.readUnsignedByte());
            log.debug("Parsed LookupStatus: {}", response.getLookupStatus());
        }
        
        // BP 1: Lookup Reject Reason (Required)
        if (isBitSet(presenceMap, 1)) {
            if (OdpConstants.BYTE_ORDER == java.nio.ByteOrder.LITTLE_ENDIAN) {
                response.setLookupRejectReason(buffer.readUnsignedShortLE());
            } else {
                response.setLookupRejectReason(buffer.readUnsignedShort());
            }
            log.debug("Parsed LookupRejectReason: {}", response.getLookupRejectReason());
        }
        
        // BP 2: IP Address 1 (Optional - present if Accepted)
        if (isBitSet(presenceMap, 2)) {
            response.setIpAddress1(readFixedString(buffer, OdpConstants.IP_ADDRESS_SIZE));
            log.debug("Parsed IpAddress1: {}", response.getIpAddress1());
        }
        
        // BP 3: Port Number 1 (Optional - present if Accepted)
        if (isBitSet(presenceMap, 3)) {
            if (OdpConstants.BYTE_ORDER == java.nio.ByteOrder.LITTLE_ENDIAN) {
                response.setPortNumber1(buffer.readUnsignedShortLE());
            } else {
                response.setPortNumber1(buffer.readUnsignedShort());
            }
            log.debug("Parsed PortNumber1: {}", response.getPortNumber1());
        }
        
        // BP 4: IP Address 2 (Optional - present if Accepted)
        if (isBitSet(presenceMap, 4)) {
            response.setIpAddress2(readFixedString(buffer, OdpConstants.IP_ADDRESS_SIZE));
            log.debug("Parsed IpAddress2: {}", response.getIpAddress2());
        }
        
        // BP 5: Port Number 2 (Optional - present if Accepted)
        if (isBitSet(presenceMap, 5)) {
            if (OdpConstants.BYTE_ORDER == java.nio.ByteOrder.LITTLE_ENDIAN) {
                response.setPortNumber2(buffer.readUnsignedShortLE());
            } else {
                response.setPortNumber2(buffer.readUnsignedShort());
            }
            log.debug("Parsed PortNumber2: {}", response.getPortNumber2());
        }
        
        log.info("Parsed LookupResponse: {}", response);
        return response;
    }
    
    /**
     * 检查 Presence Map 中指定位是否设置
     * Bit 0 是最高位（MSB）
     */
    public static boolean isBitSet(byte[] presenceMap, int bitPosition) {
        int byteIndex = bitPosition / 8;
        int bitIndex = 7 - (bitPosition % 8);  // MSB first
        
        if (byteIndex >= presenceMap.length) {
            return false;
        }
        
        boolean isSet = ((presenceMap[byteIndex] >> bitIndex) & 1) == 1;
        log.trace("isBitSet: position={}, byteIndex={}, bitIndex={}, result={}", 
                bitPosition, byteIndex, bitIndex, isSet);
        return isSet;
    }
    
    /**
     * 读取固定长度字符串，去除尾部的 0x00 和空格
     */
    private static String readFixedString(ByteBuf buffer, int length) {
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        
        // 找到有效字符串长度
        int effectiveLength = length;
        while (effectiveLength > 0 && (bytes[effectiveLength - 1] == 0x00 || bytes[effectiveLength - 1] == 0x20)) {
            effectiveLength--;
        }
        
        return new String(bytes, 0, effectiveLength, StandardCharsets.US_ASCII);
    }
    
    /**
     * 打印 Presence Map（调试用）
     */
    private static void logPresenceMap(byte[] presenceMap) {
        if (log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder("PresenceMap: ");
            for (int i = 0; i < Math.min(8, presenceMap.length); i++) {
                sb.append(String.format("%02X ", presenceMap[i]));
            }
            sb.append("| Bits set: ");
            for (int i = 0; i < 16; i++) {
                if (isBitSet(presenceMap, i)) {
                    sb.append(i).append(" ");
                }
            }
            log.debug(sb.toString());
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
            for (int i = 0; i < Math.min(buffer.readableBytes(), 256); i++) {
                if (i > 0 && i % 16 == 0) {
                    sb.append("\n");
                }
                sb.append(String.format("%02X ", buffer.getByte(readerIndex + i)));
            }
            log.trace(sb.toString());
        }
    }
}