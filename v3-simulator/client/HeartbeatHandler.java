// src/main/java/com/odp/simulator/client/handler/HeartbeatHandler.java
package com.odp.simulator.client.handler;

import com.odp.simulator.client.protocol.OdpMessage;
import com.odp.simulator.client.protocol.OdpMessageHeader;
import com.odp.simulator.client.protocol.OdpMessageType;
import com.odp.simulator.client.protocol.messages.HeartbeatMessage;
import com.odp.simulator.client.session.OdpSession;
import com.odp.simulator.client.session.OdpSessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler for heartbeat functionality
 * 
 * This handler:
 * 1. Sends heartbeat messages when the channel is idle (writer idle)
 * 2. Handles incoming heartbeat messages
 * 3. Detects if no data is received for too long (reader idle)
 */
@Slf4j
@RequiredArgsConstructor
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {

    private final OdpSessionManager sessionManager;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleEvent) {
            OdpSession session = sessionManager.getPrimarySession();
            
            if (idleEvent.state() == IdleState.WRITER_IDLE) {
                // No message sent for heartbeat interval - send heartbeat
                log.debug("Writer idle - sending heartbeat");
                sendHeartbeat(ctx, session);
            } else if (idleEvent.state() == IdleState.READER_IDLE) {
                // No message received for too long - connection might be dead
                log.warn("Reader idle - no response from server for extended period");
                // Could trigger reconnection logic here
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private void sendHeartbeat(ChannelHandlerContext ctx, OdpSession session) {
        HeartbeatMessage heartbeat = new HeartbeatMessage();
        
        OdpMessageHeader header = OdpMessageHeader.builder()
                .messageId(OdpMessageType.HEARTBEAT.getMessageId())
                .msgSeqNum(session.getNextOutgoingSeqNum())
                .compId(session.getCompId())
                .messageFlags((byte) 0)
                .fieldsPresenceMap(heartbeat.getFieldsPresenceMap())
                .build();
        
        heartbeat.setHeader(header);
        
        ctx.writeAndFlush(heartbeat);
        session.updateLastSentTime();
        
        log.trace("Heartbeat sent, seqNum: {}", header.getMsgSeqNum());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HeartbeatMessage) {
            log.trace("Heartbeat received");
            OdpSession session = sessionManager.getPrimarySession();
            session.updateLastReceivedTime();
            // Heartbeat doesn't need further processing
        } else {
            // Pass other messages to the next handler
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("HeartbeatHandler exception: {}", cause.getMessage(), cause);
        ctx.fireExceptionCaught(cause);
    }
}