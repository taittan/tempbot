// src/main/java/com/odp/simulator/client/client/OdpTradingClient.java
package com.odp.simulator.client.client;

import com.odp.simulator.client.codec.OdpMessageDecoder;
import com.odp.simulator.client.codec.OdpMessageEncoder;
import com.odp.simulator.client.config.OdpClientProperties;
import com.odp.simulator.client.crypto.OdpPasswordEncryptor;
import com.odp.simulator.client.handler.HeartbeatHandler;
import com.odp.simulator.client.handler.LogonResponseHandler;
import com.odp.simulator.client.handler.OdpClientHandler;
import com.odp.simulator.client.protocol.OdpMessage;
import com.odp.simulator.client.protocol.OdpMessageHeader;
import com.odp.simulator.client.protocol.OdpMessageType;
import com.odp.simulator.client.protocol.messages.LogonRequest;
import com.odp.simulator.client.protocol.messages.LogonResponse;
import com.odp.simulator.client.session.OdpSession;
import com.odp.simulator.client.session.OdpSessionManager;
import com.odp.simulator.client.session.OdpSessionState;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Client for connecting to ODP Trading Gateway
 * 
 * Handles:
 * - Connection to trading gateway
 * - Logon/Logout
 * - Heartbeat maintenance
 * - Order operations (to be implemented)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OdpTradingClient {

    private final EventLoopGroup eventLoopGroup;
    private final OdpClientProperties properties;
    private final OdpSessionManager sessionManager;
    private final OdpPasswordEncryptor passwordEncryptor;
    private final LogonResponseHandler logonResponseHandler;

    private volatile Channel tradingChannel;
    private OdpClientHandler clientHandler;

    /**
     * Connect to trading gateway and perform logon
     */
    public LogonResponse connectAndLogon(String host, int port) throws Exception {
        // Ensure public key is loaded for password encryption
        if (!passwordEncryptor.isKeyLoaded()) {
            passwordEncryptor.loadPublicKey(properties.getPublicKeyPath());
        }

        OdpSession session = sessionManager.getPrimarySession();
        session.transitionTo(OdpSessionState.CONNECTING);

        CompletableFuture<LogonResponse> logonFuture = new CompletableFuture<>();
        logonResponseHandler.setLogonFuture(logonFuture);

        int heartbeatSeconds = properties.getHeartbeatIntervalSeconds();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getTrading().getConnectTimeoutMs())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        
                        // Logging handler for debugging
                        pipeline.addLast("logging", new LoggingHandler(LogLevel.DEBUG));
                        
                        // Read timeout - longer than heartbeat interval
                        pipeline.addLast("readTimeout", 
                                new ReadTimeoutHandler(properties.getTrading().getReadTimeoutMs(), TimeUnit.MILLISECONDS));
                        
                        // Idle state handler for heartbeat
                        // Writer idle triggers heartbeat, reader idle triggers warning
                        pipeline.addLast("idleState", 
                                new IdleStateHandler(heartbeatSeconds * 3, heartbeatSeconds, 0));
                        
                        // Codec
                        pipeline.addLast("decoder", new OdpMessageDecoder());
                        pipeline.addLast("encoder", new OdpMessageEncoder());
                        
                        // Heartbeat handler
                        pipeline.addLast("heartbeat", new HeartbeatHandler(sessionManager));
                        
                        // Main client handler
                        clientHandler = new OdpClientHandler(sessionManager);
                        clientHandler.registerHandler(logonResponseHandler);
                        // TODO: Register more handlers for order messages
                        // clientHandler.registerHandler(orderAcceptedHandler);
                        // clientHandler.registerHandler(orderRejectedHandler);
                        // clientHandler.registerHandler(executionReportHandler);
                        pipeline.addLast("handler", clientHandler);
                    }
                });

        // Connect to trading gateway
        log.info("Connecting to trading gateway at {}:{}", host, port);
        ChannelFuture connectFuture = bootstrap.connect(host, port);
        connectFuture.sync();
        tradingChannel = connectFuture.channel();
        session.setChannel(tradingChannel);

        log.info("Connected to trading gateway: {}", tradingChannel.remoteAddress());

        // Send logon request
        session.transitionTo(OdpSessionState.LOGON_PENDING);
        sendLogonRequest();

        // Wait for logon response
        LogonResponse response = logonFuture.get(
                properties.getTrading().getConnectTimeoutMs(), TimeUnit.MILLISECONDS);

        return response;
    }

    private void sendLogonRequest() throws Exception {
        OdpSession session = sessionManager.getPrimarySession();

        // Encrypt password with login time prefix
        String encryptedPassword = passwordEncryptor.encryptPassword(properties.getPassword());

        LogonRequest request = LogonRequest.builder()
                .password(encryptedPassword)
                .heartbeatInterval(properties.getHeartbeatIntervalSeconds())
                .nextExpectedMsgSeqNum(session.getExpectedIncomingSeqNum())
                .epApplVersionId(properties.getEpApplVersionId())
                .build();

        OdpMessageHeader header = OdpMessageHeader.builder()
                .messageId(OdpMessageType.LOGON_REQUEST.getMessageId())
                .msgSeqNum(session.getNextOutgoingSeqNum())
                .compId(properties.getCompId())
                .messageFlags((byte) 0)
                .fieldsPresenceMap(request.getFieldsPresenceMap())
                .build();
        request.setHeader(header);

        log.debug("Sending logon request for Comp ID: {}", properties.getCompId());
        tradingChannel.writeAndFlush(request);
        session.updateLastSentTime();
    }

    /**
     * Send a message to the trading gateway
     */
    public void sendMessage(OdpMessage message) {
        if (tradingChannel == null || !tradingChannel.isActive()) {
            throw new IllegalStateException("Trading channel is not active");
        }

        OdpSession session = sessionManager.getPrimarySession();
        if (!session.isActive()) {
            throw new IllegalStateException("Session is not active");
        }

        // Set sequence number if not already set
        if (message.getHeader().getMsgSeqNum() == 0) {
            message.getHeader().setMsgSeqNum(session.getNextOutgoingSeqNum());
        }

        tradingChannel.writeAndFlush(message);
        session.updateLastSentTime();
    }

    /**
     * Check if connected to trading gateway
     */
    public boolean isConnected() {
        return tradingChannel != null && tradingChannel.isActive();
    }

    /**
     * Close the trading connection
     */
    public void disconnect() {
        if (tradingChannel != null) {
            tradingChannel.close();
            tradingChannel = null;
        }
    }
}