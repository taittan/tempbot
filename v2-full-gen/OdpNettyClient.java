package com.example.odp.client;

import com.example.odp.codec.OdpFrameDecoder;
import com.example.odp.codec.OdpMessageDecoder;
import com.example.odp.codec.OdpMessageEncoder;
import com.example.odp.config.OdpClientConfig;
import com.example.odp.handler.HeartbeatHandler;
import com.example.odp.handler.OdpClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OdpNettyClient {
    
    private final OdpClientConfig config;
    
    private EventLoopGroup workerGroup;
    private Bootstrap bootstrap;
    private Channel channel;
    
    private volatile boolean running = false;
    
    public OdpNettyClient(OdpClientConfig config) {
        this.config = config;
    }
    
    @PostConstruct
    public void init() {
        log.info("Initializing ODP Netty Client...");
        log.info("Target: {}:{}", config.getHost(), config.getPort());
        log.info("CompId: {}", config.getCompId());
        log.info("Heartbeat Interval: {} seconds", config.getHeartbeatInterval());
        
        workerGroup = new NioEventLoopGroup();
        
        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeout() * 1000)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        
                        log.debug("Initializing channel pipeline...");
                        
                        // ========== 关键：添加 LoggingHandler 用于调试 ==========
                        // 这会打印所有入站和出站的原始字节数据
                        pipeline.addLast("logging", new LoggingHandler(LogLevel.DEBUG));
                        
                        // ========== 帧解码器 - 处理粘包/拆包 ==========
                        pipeline.addLast("frameDecoder", new OdpFrameDecoder());
                        
                        // ========== 消息解码器 - ByteBuf -> 消息对象 ==========
                        pipeline.addLast("messageDecoder", new OdpMessageDecoder());
                        
                        // ========== 消息编码器 ==========
                        pipeline.addLast("messageEncoder", new OdpMessageEncoder());
                        
                        // ========== 空闲检测 - 用于心跳 ==========
                        pipeline.addLast("idleStateHandler", new IdleStateHandler(
                                config.getReadTimeout(),      // 读空闲时间
                                config.getHeartbeatInterval(), // 写空闲时间（触发心跳）
                                0,                            // 全部空闲时间
                                TimeUnit.SECONDS
                        ));
                        
                        // ========== 心跳处理器 ==========
                        pipeline.addLast("heartbeatHandler", new HeartbeatHandler(config));
                        
                        // ========== 业务处理器 ==========
                        pipeline.addLast("clientHandler", new OdpClientHandler(config));
                        
                        log.debug("Pipeline initialized: {}", pipeline.names());
                    }
                });
        
        // 启动连接
        connect();
    }
    
    public void connect() {
        if (running) {
            log.warn("Client is already running");
            return;
        }
        
        log.info("Connecting to ODP Gateway at {}:{}...", config.getHost(), config.getPort());
        
        bootstrap.connect(config.getHost(), config.getPort())
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        channel = future.channel();
                        running = true;
                        log.info("Successfully connected to ODP Gateway!");
                        log.info("Channel: {}", channel);
                        log.info("Channel active: {}", channel.isActive());
                        log.info("Channel open: {}", channel.isOpen());
                        
                        // 监听 channel 关闭事件
                        channel.closeFuture().addListener((ChannelFutureListener) closeFuture -> {
                            log.warn("Channel closed!");
                            running = false;
                            
                            if (config.isReconnectEnabled()) {
                                scheduleReconnect();
                            }
                        });
                        
                    } else {
                        log.error("Failed to connect to ODP Gateway: {}", future.cause().getMessage());
                        running = false;
                        
                        if (config.isReconnectEnabled()) {
                            scheduleReconnect();
                        }
                    }
                });
    }
    
    private void scheduleReconnect() {
        log.info("Scheduling reconnect in {} seconds...", config.getReconnectDelay());
        
        workerGroup.schedule(() -> {
            log.info("Attempting to reconnect...");
            connect();
        }, config.getReconnectDelay(), TimeUnit.SECONDS);
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down ODP Netty Client...");
        
        running = false;
        
        if (channel != null && channel.isActive()) {
            channel.close().syncUninterruptibly();
        }
        
        if (workerGroup != null) {
            workerGroup.shutdownGracefully().syncUninterruptibly();
        }
        
        log.info("ODP Netty Client shutdown complete");
    }
    
    public boolean isConnected() {
        return channel != null && channel.isActive();
    }
    
    public Channel getChannel() {
        return channel;
    }
}