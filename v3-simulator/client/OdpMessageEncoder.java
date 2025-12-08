// src/main/java/com/odp/simulator/client/codec/OdpMessageEncoder.java
package com.odp.simulator.client.codec;

import com.odp.simulator.client.protocol.OdpDataType;
import com.odp.simulator.client.protocol.OdpMessage;
import com.odp.simulator.client.protocol.OdpMessageHeader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * Encoder for ODP messages
 * 
 * Encodes OdpMessage objects into binary format according to ODP protocol.
 * 
 * Message structure:
 * - Length (UInt16, Little Endian) - Total message length including header
 * - Message ID (UInt16, Little Endian)
 * - MsgSeqNum (UInt64, Little Endian)
 * - Comp ID (Char Array 11)
 * - Message Flags (UInt8)
 * - Fields Presence Map (Bitmap 12)
 * - Body fields (based on presence map)
 */
@Slf4j
public class OdpMessageEncoder extends MessageToByteEncoder<OdpMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, OdpMessage msg, ByteBuf out) throws Exception {
        log.debug("Encoding message: type={}, seqNum={}", 
                msg.getMessageType(), msg.getHeader().getMsgSeqNum());

        OdpMessageHeader header = msg.getHeader();
        byte[] presenceMap = msg.getFieldsPresenceMap();
        int bodyLength = msg.calculateBodyLength();
        int totalLength = OdpDataType.HEADER_SIZE + bodyLength;

        // Update header with calculated length
        header.setLength(totalLength);
        header.setFieldsPresenceMap(presenceMap);

        log.debug("Message length: header={}, body={}, total={}", 
                OdpDataType.HEADER_SIZE, bodyLength, totalLength);

        // Encode header
        encodeHeader(out, header);

        // Encode body
        int bodyStartIndex = out.writerIndex();
        msg.encodeBody(out);
        int actualBodyLength = out.writerIndex() - bodyStartIndex;

        log.debug("Encoded message: totalLength={}, actualBodyLength={}", totalLength, actualBodyLength);

        // Log the encoded bytes for debugging
        if (log.isTraceEnabled()) {
            byte[] bytes = new byte[out.readableBytes()];
            out.getBytes(out.readerIndex(), bytes);
            log.trace("Encoded bytes: {}", bytesToHex(bytes));
        }
    }

    private void encodeHeader(ByteBuf buffer, OdpMessageHeader header) {
        // Length (UInt16 Little Endian)
        buffer.writeShortLE(header.getLength());

        // Message ID (UInt16 Little Endian)
        buffer.writeShortLE(header.getMessageId());

        // MsgSeqNum (UInt64 Little Endian)
        buffer.writeLongLE(header.getMsgSeqNum());

        // Comp ID (Char Array 11)
        writeCharArray(buffer, header.getCompId(), OdpDataType.COMP_ID_SIZE);

        // Message Flags (UInt8)
        buffer.writeByte(header.getMessageFlags());

        // Fields Presence Map (Bitmap 12)
        byte[] presenceMap = header.getFieldsPresenceMap();
        if (presenceMap == null) {
            presenceMap = new byte[OdpDataType.FIELDS_PRESENCE_MAP_SIZE];
        }
        buffer.writeBytes(presenceMap);

        log.debug("Encoded header: length={}, msgId={}, seqNum={}, compId={}, flags=0x{}, presenceMap={}", 
                header.getLength(), header.getMessageId(), header.getMsgSeqNum(),
                header.getCompId(), String.format("%02X", header.getMessageFlags()),
                bytesToHex(presenceMap));
    }

    private void writeCharArray(ByteBuf buffer, String value, int length) {
        byte[] bytes = new byte[length];
        if (value != null && !value.isEmpty()) {
            byte[] valueBytes = value.getBytes(StandardCharsets.US_ASCII);
            int copyLength = Math.min(valueBytes.length, length);
            System.arraycopy(valueBytes, 0, bytes, 0, copyLength);
        }
        buffer.writeBytes(bytes);
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