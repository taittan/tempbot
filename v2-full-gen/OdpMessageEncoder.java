package com.example.odp.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * 消息编码器 - 如果需要对发送的消息做统一处理
 * 目前直接透传 ByteBuf
 */
@Slf4j
public class OdpMessageEncoder extends MessageToByteEncoder<ByteBuf> {
    
    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        log.debug("OdpMessageEncoder.encode called, message bytes: {}", msg.readableBytes());
        logHexDump("Outgoing Message", msg);
        
        out.writeBytes(msg);
    }
    
    private void logHexDump(String name, ByteBuf buffer) {
        if (log.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append(" (").append(buffer.readableBytes()).append(" bytes): ");
            
            int readerIndex = buffer.readerIndex();
            for (int i = 0; i < buffer.readableBytes(); i++) {
                sb.append(String.format("%02X ", buffer.getByte(readerIndex + i)));
            }
            log.trace(sb.toString());
        }
    }
}