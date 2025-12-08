// src/main/java/com/odp/simulator/client/codec/OdpMessageDecoder.java
package com.odp.simulator.client.codec;

import com.odp.simulator.client.protocol.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Decoder for ODP messages
 * 
 * Decodes binary data into OdpMessage objects according to ODP protocol.
 * 
 * Uses length-prefix framing to handle message boundaries.
 */
@Slf4j
public class OdpMessageDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // Need at least 2 bytes for length field
        if (in.readableBytes() < OdpDataType.LENGTH_SIZE) {
            log.trace("Not enough bytes for length field, available: {}", in.readableBytes());
            return;
        }

        // Mark the current position
        in.markReaderIndex();

        // Read length (UInt16 Little Endian)
        int length = in.readShortLE() & 0xFFFF;

        log.debug("Decoding message, length field: {}, readable bytes: {}", length, in.readableBytes());

        // Check if we have the complete message (length includes the 2-byte length field)
        if (in.readableBytes() < length - OdpDataType.LENGTH_SIZE) {
            log.debug("Incomplete message, need {} more bytes", 
                    (length - OdpDataType.LENGTH_SIZE) - in.readableBytes());
            in.resetReaderIndex();
            return;
        }

        // Read Message ID (UInt16 Little Endian)
        int messageId = in.readShortLE() & 0xFFFF;
        OdpMessageType messageType = OdpMessageType.fromMessageId(messageId);
        
        log.debug("Message ID: {} ({})", messageId, messageType);

        // Read MsgSeqNum (UInt64 Little Endian)
        long msgSeqNum = in.readLongLE();

        // Read Comp ID (Char Array 11)
        String compId = readCharArray(in, OdpDataType.COMP_ID_SIZE);

        // Read Message Flags (UInt8)
        byte messageFlags = in.readByte();

        // Read Fields Presence Map (Bitmap 12)
        byte[] presenceMap = new byte[OdpDataType.FIELDS_PRESENCE_MAP_SIZE];
        in.readBytes(presenceMap);

        log.debug("Decoded header: length={}, msgId={}, seqNum={}, compId={}, flags=0x{}, presenceMap={}", 
                length, messageId, msgSeqNum, compId, 
                String.format("%02X", messageFlags), bytesToHex(presenceMap));

        // Create header
        OdpMessageHeader header = OdpMessageHeader.builder()
                .length(length)
                .messageId(messageId)
                .msgSeqNum(msgSeqNum)
                .compId(compId)
                .messageFlags(messageFlags)
                .fieldsPresenceMap(presenceMap)
                .build();

        // Create message instance
        OdpMessage message = OdpMessageFactory.createMessage(messageType);
        if (message == null) {
            log.warn("Unknown message type: {}, skipping {} bytes", messageType, 
                    length - OdpDataType.HEADER_SIZE);
            // Skip the remaining body bytes
            in.skipBytes(length - OdpDataType.HEADER_SIZE);
            return;
        }

        message.setHeader(header);

        // Decode body
        int bodyStartIndex = in.readerIndex();
        int expectedBodyLength = length - OdpDataType.HEADER_SIZE;
        
        try {
            message.decodeBody(in, presenceMap);
            
            int actualBytesRead = in.readerIndex() - bodyStartIndex;
            if (actualBytesRead < expectedBodyLength) {
                log.debug("Body decoder read {} bytes, expected {}, skipping remaining {} bytes",
                        actualBytesRead, expectedBodyLength, expectedBodyLength - actualBytesRead);
                in.skipBytes(expectedBodyLength - actualBytesRead);
            }
        } catch (Exception e) {
            log.error("Error decoding message body for type {}: {}", messageType, e.getMessage(), e);
            // Skip remaining bytes to prevent corruption
            int bytesToSkip = expectedBodyLength - (in.readerIndex() - bodyStartIndex);
            if (bytesToSkip > 0) {
                in.skipBytes(bytesToSkip);
            }
            throw e;
        }

        log.debug("Successfully decoded message: {}", messageType);
        out.add(message);
    }

    private String readCharArray(ByteBuf buffer, int length) {
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        
        int strLen = 0;
        for (int i = 0; i < length; i++) {
            if (bytes[i] == 0) {
                break;
            }
            strLen++;
        }
        
        if (strLen == 0) {
            return null;
        }
        
        return new String(bytes, 0, strLen, StandardCharsets.US_ASCII);
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}