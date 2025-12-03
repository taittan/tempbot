package com.example.odp.handler;

import com.example.odp.builder.OdpMessageBuilder;
import com.example.odp.config.OdpClientConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 心跳处理器 - 发送心跳保持连接活跃
 */
@Slf4j
public class HeartbeatHandler extends ChannelDuplexHandler {
    
    private final OdpClientConfig config;
    private final AtomicLong heartbeatSeqNum = new AtomicLong(1);
    
    public HeartbeatHandler(OdpClientConfig config) {
        this.config = config;
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            
            if (event.state() == IdleState.WRITER_IDLE) {
                // 写空闲，发送心跳
                sendHeartbeat(ctx);
            } else if (event.state() == IdleState.READER_IDLE) {
                // 读空闲，可能服务端已断开
                log.warn("Read idle detected, server may be unresponsive");
                // 可以选择发送 Test Request 或关闭连接
            } else if (event.state() == IdleState.ALL_IDLE) {
                log.warn("All idle detected");
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
    
    private void sendHeartbeat(ChannelHandlerContext ctx) {
        if (ctx.channel().isActive()) {
            long seqNum = heartbeatSeqNum.getAndIncrement();
            
            ByteBuf heartbeat = OdpMessageBuilder.buildHeartbeat(
                    ctx.alloc(), 
                    config.getCompId(), 
                    seqNum
            );
            
            log.debug("Sending Heartbeat, seqNum={}", seqNum);
            ctx.writeAndFlush(heartbeat).addListener(future -> {
                if (future.isSuccess()) {
                    log.trace("Heartbeat sent successfully, seqNum={}", seqNum);
                } else {
                    log.error("Failed to send Heartbeat", future.cause());
                }
            });
        } else {
            log.warn("Channel is not active, cannot send heartbeat");
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("HeartbeatHandler exception: ", cause);
        super.exceptionCaught(ctx, cause);
    }
}