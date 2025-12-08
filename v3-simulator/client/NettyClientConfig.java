// src/main/java/com/odp/simulator/client/config/NettyClientConfig.java
package com.odp.simulator.client.config;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NettyClientConfig {

    /**
     * Shared event loop group for all Netty clients
     * Using a reasonable number of threads for client connections
     */
    @Bean(destroyMethod = "shutdownGracefully")
    public EventLoopGroup eventLoopGroup() {
        // Use 2 threads for client connections (lookup + trading)
        return new NioEventLoopGroup(2);
    }
}