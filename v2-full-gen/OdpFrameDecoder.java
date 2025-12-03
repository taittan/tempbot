package com.example.odp.codec;

import com.example.odp.constants.OdpConstants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 帧解码器 - 根据消息头的 Length 字段拆包
 * 这是排查问题的关键！确保正确读取完整消息
 */
@Slf4j
public class OdpFrameDecoder extends ByteToMessageDecoder {
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        log.trace("OdpFrameDecoder.decode called, readable bytes: {}", in.readableBytes());
        
        // 需要至少 2 个字节来读取 Length 字段
        if (in.readableBytes() < OdpConstants.LENGTH_SIZE) {
            log.trace("Not enough bytes to read Length field, waiting...");
            return;
        }
        
        // 标记当前读取位置
        in.markReaderIndex();
        
        // 读取 Length 字段（不移动 readerIndex，使用 get 而非 read）
        int length;
        if (OdpConstants.BYTE_ORDER == java.nio.ByteOrder.LITTLE_ENDIAN) {
            length = in.readUnsignedShortLE();
        } else {
            length = in.readUnsignedShort();
        }
        
        log.debug("OdpFrameDecoder: Message Length from header = {}", length);
        
        // 校验长度合理性
        if (length < OdpConstants.HEADER_SIZE) {
            log.error("Invalid message length: {} (minimum should be {})", length, OdpConstants.HEADER_SIZE);
            // 重置读取位置，跳过这个异常数据
            in.resetReaderIndex();
            in.skipBytes(1);  // 跳过一个字节，尝试重新同步
            return;
        }
        
        if (length > 65535) {
            log.error("Message length too large: {}", length);
            in.resetReaderIndex();
            in.skipBytes(1);
            return;
        }
        
        // 检查是否有足够的数据
        // 注意：我们已经读了 2 个字节（Length），所以还需要 (length - 2) 个字节
        int remaining = length - OdpConstants.LENGTH_SIZE;
        if (in.readableBytes() < remaining) {
            log.debug("Not enough bytes for complete message. Have: {}, Need: {}", 
                    in.readableBytes(), remaining);
            // 重置读取位置，等待更多数据
            in.resetReaderIndex();
            return;
        }
        
        // 重置到消息开始位置
        in.resetReaderIndex();
        
        // 读取完整消息
        ByteBuf frame = in.readRetainedSlice(length);
        
        log.debug("OdpFrameDecoder: Complete frame extracted, length = {}", length);
        logHexDump("Extracted Frame", frame);
        
        out.add(frame);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("OdpFrameDecoder exception: ", cause);
        super.exceptionCaught(ctx, cause);
    }
    
    private void logHexDump(String name, ByteBuf buffer) {
        if (log.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append(" (").append(buffer.readableBytes()).append(" bytes): ");
            
            int readerIndex = buffer.readerIndex();
            int limit = Math.min(buffer.readableBytes(), 64);
            for (int i = 0; i < limit; i++) {
                sb.append(String.format("%02X ", buffer.getByte(readerIndex + i)));
            }
            if (buffer.readableBytes() > 64) {
                sb.append("...");
            }
            log.trace(sb.toString());
        }
    }
}