// src/main/java/com/odp/simulator/client/handler/OdpClientHandler.java
package com.odp.simulator.client.handler;

import com.odp.simulator.client.protocol.OdpMessage;
import com.odp.simulator.client.protocol.OdpMessageType;
import com.odp.simulator.client.session.OdpSession;
import com.odp.simulator.client.session.OdpSessionManager;
import com.odp.simulator.client.session.OdpSessionState;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main handler for ODP client
 * 
 * Routes incoming messages to appropriate handlers based on message type.
 * 
 * This handler is shared across connections and delegates to specific
 * message handlers registered in the handler map.
 */
@Slf4j
@RequiredArgsConstructor
public class OdpClientHandler extends ChannelInboundHandlerAdapter {

    private final OdpSessionManager sessionManager;
    private final Map<OdpMessageType, OdpMessageHandler> handlers = new ConcurrentHashMap<>();

    /**
     * Register a message handler
     */
    public void registerHandler(OdpMessageHandler handler) {
        handlers.put(handler.getMessageType(), handler);
        log.debug("Registered handler for message type: {}", handler.getMessageType());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Channel active: {}", ctx.channel().remoteAddress());
        OdpSession session = sessionManager.getPrimarySession();
        session.setChannel(ctx.channel());
        session.updateLastReceivedTime();
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Channel inactive: {}", ctx.channel().remoteAddress());
        OdpSession session = sessionManager.getPrimarySession();
        
        if (session.getState() != OdpSessionState.LOGGED_OUT) {
            session.transitionTo(OdpSessionState.DISCONNECTED);
        }
        
        session.setChannel(null);
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof OdpMessage message)) {
            log.warn("Received unexpected message type: {}", msg.getClass().getSimpleName());
            return;
        }

        OdpMessageType messageType = message.getMessageType();
        log.debug("Received message: type={}, seqNum={}", 
                messageType, message.getHeader().getMsgSeqNum());

        OdpSession session = sessionManager.getPrimarySession();
        session.updateLastReceivedTime();
        session.incrementExpectedIncomingSeqNum();

        // Find and invoke the appropriate handler
        OdpMessageHandler handler = handlers.get(messageType);
        if (handler != null) {
            try {
                handler.handle(message);
            } catch (Exception e) {
                log.error("Error handling message type {}: {}", messageType, e.getMessage(), e);
            }
        } else {
            log.warn("No handler registered for message type: {}", messageType);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Channel exception: {}", cause.getMessage(), cause);
        
        OdpSession session = sessionManager.getPrimarySession();
        session.transitionTo(OdpSessionState.ERROR);
        
        ctx.close();
    }
}