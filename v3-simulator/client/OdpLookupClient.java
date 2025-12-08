// src/main/java/com/odp/simulator/client/client/OdpLookupClient.java
package com.odp.simulator.client.client;

import com.odp.simulator.client.codec.OdpMessageDecoder;
import com.odp.simulator.client.codec.OdpMessageEncoder;
import com.odp.simulator.client.config.OdpClientProperties;
import com.odp.simulator.client.handler.LookupResponseHandler;
import com.odp.simulator.client.handler.OdpClientHandler;
import com.odp.simulator.client.protocol.OdpMessageHeader;
import com.odp.simulator.client.protocol.OdpMessageType;
import com.odp.simulator.client.protocol.messages.LookupRequest;
import com.odp.simulator.client.protocol.messages.LookupResponse;
import com.odp.simulator.client.session.OdpSession;
import com.odp.simulator.client.session.OdpSessionManager;
import com.odp.simulator.client.session.OdpSessionState;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Client for connecting to ODP Lookup Service
 * 
 * The lookup service provides the gateway connection point (IP/Port)
 * for a given Comp ID.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OdpLookupClient {

    private final EventLoopGroup eventLoopGroup;
    private final OdpClientProperties properties;
    private final OdpSessionManager sessionManager;
    private final LookupResponseHandler lookupResponseHandler;

    /**
     * Perform lookup to get gateway connection info
     * 
     * Attempts lookup services in order:
     * 1. Primary site primary
     * 2. Primary site secondary
     * 3. Secondary site primary
     * 4. Secondary site secondary
     * 5. Cycle back to primary site primary
     */
    public LookupResponse performLookup() throws Exception {
        List<OdpClientProperties.EndpointConfig> lookupEndpoints = getLookupEndpoints();
        
        Exception lastException = null;
        int maxAttempts = lookupEndpoints.size() * 2; // Allow one full cycle
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            OdpClientProperties.EndpointConfig endpoint = lookupEndpoints.get(attempt % lookupEndpoints.size());
            
            log.info("Attempting lookup service {}/{}: {}:{}", 
                    attempt + 1, maxAttempts, endpoint.getHost(), endpoint.getPort());
            
            try {
                LookupResponse response = doLookup(endpoint.getHost(), endpoint.getPort());
                if (response.isAccepted()) {
                    return response;
                } else {
                    log.warn("Lookup rejected: {}", response.getRejectReasonDescription());
                    // Don't retry if rejected - this is a definitive response
                    throw new RuntimeException("Lookup rejected: " + response.getRejectReasonDescription());
                }
            } catch (Exception e) {
                log.warn("Lookup attempt {} failed: {}", attempt + 1, e.getMessage());
                lastException = e;
                
                // Wait before retry as per spec (5 seconds)
                if (attempt < maxAttempts - 1) {
                    log.info("Waiting {} ms before next attempt...", properties.getLookup().getRetryWaitMs());
                    Thread.sleep(properties.getLookup().getRetryWaitMs());
                }
            }
        }
        
        throw new RuntimeException("All lookup attempts failed", lastException);
    }

    private LookupResponse doLookup(String host, int port) throws Exception {
        OdpSession session = sessionManager.getPrimarySession();
        session.transitionTo(OdpSessionState.LOOKUP_PENDING);

        CompletableFuture<LookupResponse> responseFuture = new CompletableFuture<>();
        lookupResponseHandler.setLookupFuture(responseFuture);

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getLookup().getConnectTimeoutMs())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        
                        // Logging handler for debugging
                        pipeline.addLast("logging", new LoggingHandler(LogLevel.DEBUG));
                        
                        // Read timeout
                        pipeline.addLast("readTimeout", 
                                new ReadTimeoutHandler(properties.getLookup().getConnectTimeoutMs(), TimeUnit.MILLISECONDS));
                        
                        // Codec
                        pipeline.addLast("decoder", new OdpMessageDecoder());
                        pipeline.addLast("encoder", new OdpMessageEncoder());
                        
                        // Handler
                        OdpClientHandler clientHandler = new OdpClientHandler(sessionManager);
                        clientHandler.registerHandler(lookupResponseHandler);
                        pipeline.addLast("handler", clientHandler);
                    }
                });

        Channel channel = null;
        try {
            // Connect to lookup service
            log.debug("Connecting to lookup service at {}:{}", host, port);
            ChannelFuture connectFuture = bootstrap.connect(host, port);
            connectFuture.sync();
            channel = connectFuture.channel();
            
            log.info("Connected to lookup service: {}", channel.remoteAddress());

            // Send lookup request
            LookupRequest request = new LookupRequest();
            OdpMessageHeader header = OdpMessageHeader.builder()
                    .messageId(OdpMessageType.LOOKUP_REQUEST.getMessageId())
                    .msgSeqNum(1) // Must be 1 for lookup request
                    .compId(properties.getCompId())
                    .messageFlags((byte) 0)
                    .fieldsPresenceMap(request.getFieldsPresenceMap())
                    .build();
            request.setHeader(header);

            log.debug("Sending lookup request for Comp ID: {}", properties.getCompId());
            channel.writeAndFlush(request);
            session.updateLastSentTime();

            // Wait for response
            LookupResponse response = responseFuture.get(
                    properties.getLookup().getConnectTimeoutMs(), TimeUnit.MILLISECONDS);
            
            return response;
            
        } finally {
            if (channel != null) {
                channel.close().sync();
            }
        }
    }

    private List<OdpClientProperties.EndpointConfig> getLookupEndpoints() {
        List<OdpClientProperties.EndpointConfig> endpoints = new ArrayList<>();
        
        OdpClientProperties.LookupConfig lookup = properties.getLookup();
        
        if (lookup.getPrimarySitePrimary() != null) {
            endpoints.add(lookup.getPrimarySitePrimary());
        }
        if (lookup.getPrimarySiteSecondary() != null) {
            endpoints.add(lookup.getPrimarySiteSecondary());
        }
        if (lookup.getSecondarySitePrimary() != null) {
            endpoints.add(lookup.getSecondarySitePrimary());
        }
        if (lookup.getSecondarySiteSecondary() != null) {
            endpoints.add(lookup.getSecondarySiteSecondary());
        }
        
        if (endpoints.isEmpty()) {
            throw new IllegalStateException("No lookup service endpoints configured");
        }
        
        return endpoints;
    }
}