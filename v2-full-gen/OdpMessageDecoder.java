package com.example.odp.codec;

import com.example.odp.constants.OdpConstants;
import com.example.odp.message.LookupResponse;
import com.example.odp.parser.OdpMessageParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 消息解码器 - 将 ByteBuf 转换为具体的消息对象
 */
@Slf4j
public class OdpMessageDecoder extends MessageToMessageDecoder<ByteBuf> {
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        log.debug("OdpMessageDecoder.decode called, readable bytes: {}", msg.readableBytes());
        
        if (msg.readableBytes() < OdpConstants.HEADER_SIZE) {
            log.warn("Message too short, expected at least {} bytes, got {}", 
                    OdpConstants.HEADER_SIZE, msg.readableBytes());
            return;
        }
        
        // Peek Message ID (offset = 2, Length 字段之后)
        int messageId;
        if (OdpConstants.BYTE_ORDER == java.nio.ByteOrder.LITTLE_ENDIAN) {
            messageId = msg.getUnsignedShortLE(OdpConstants.LENGTH_SIZE);
        } else {
            messageId = msg.getUnsignedShort(OdpConstants.LENGTH_SIZE);
        }
        
        log.debug("OdpMessageDecoder: Message ID = {} ({})", messageId, getMessageName(messageId));
        
        switch (messageId) {
            case OdpConstants.MSG_ID_LOOKUP_RESPONSE:
                LookupResponse lookupResponse = OdpMessageParser.parseLookupResponse(msg);
                out.add(lookupResponse);
                break;
                
            case OdpConstants.MSG_ID_HEARTBEAT:
                log.debug("Received Heartbeat from server");
                // 可以创建 Heartbeat 对象，或者直接忽略
                out.add(new HeartbeatReceived());
                break;
                
            case OdpConstants.MSG_ID_REJECT:
                log.warn("Received Reject message from server");
                // TODO: 解析 Reject 消息
                break;
                
            case OdpConstants.MSG_ID_LOGOUT:
                log.warn("Received Logout message from server");
                // TODO: 解析 Logout 消息
                break;
                
            default:
                log.warn("Unknown message type: {}, skipping...", messageId);
                logHexDump("Unknown Message", msg);
                break;
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("OdpMessageDecoder exception: ", cause);
        super.exceptionCaught(ctx, cause);
    }
    
    private String getMessageName(int messageId) {
        return switch (messageId) {
            case OdpConstants.MSG_ID_HEARTBEAT -> "Heartbeat";
            case OdpConstants.MSG_ID_TEST_REQUEST -> "TestRequest";
            case OdpConstants.MSG_ID_RESEND_REQUEST -> "ResendRequest";
            case OdpConstants.MSG_ID_REJECT -> "Reject";
            case OdpConstants.MSG_ID_SEQUENCE_RESET -> "SequenceReset";
            case OdpConstants.MSG_ID_LOGON_REQUEST -> "LogonRequest";
            case OdpConstants.MSG_ID_LOGON_RESPONSE -> "LogonResponse";
            case OdpConstants.MSG_ID_LOGOUT -> "Logout";
            case OdpConstants.MSG_ID_BUSINESS_MESSAGE_REJECT -> "BusinessMessageReject";
            case OdpConstants.MSG_ID_LOOKUP_REQUEST -> "LookupRequest";
            case OdpConstants.MSG_ID_LOOKUP_RESPONSE -> "LookupResponse";
            default -> "Unknown(" + messageId + ")";
        };
    }
    
    private void logHexDump(String name, ByteBuf buffer) {
        if (log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append(" HexDump: ");
            
            int readerIndex = buffer.readerIndex();
            int limit = Math.min(buffer.readableBytes(), 128);
            for (int i = 0; i < limit; i++) {
                sb.append(String.format("%02X ", buffer.getByte(readerIndex + i)));
            }
            log.debug(sb.toString());
        }
    }
    
    /**
     * 标记类，用于表示收到了心跳
     */
    public static class HeartbeatReceived {
        @Override
        public String toString() {
            return "HeartbeatReceived";
        }
    }
}