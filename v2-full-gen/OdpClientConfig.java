package com.example.odp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "odp.client")
public class OdpClientConfig {
    
    private String host;
    private int port;
    private String compId;
    
    private int heartbeatInterval = 3;
    private int connectTimeout = 10;
    private int readTimeout = 30;
    
    private boolean reconnectEnabled = true;
    private int reconnectDelay = 5;
}