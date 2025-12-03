package com.example.odp.service;

import com.example.odp.client.OdpNettyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OdpClientService {
    
    private final OdpNettyClient nettyClient;
    
    public OdpClientService(OdpNettyClient nettyClient) {
        this.nettyClient = nettyClient;
    }
    
    public boolean isConnected() {
        return nettyClient.isConnected();
    }
    
    public void reconnect() {
        if (!nettyClient.isConnected()) {
            nettyClient.connect();
        } else {
            log.info("Client is already connected");
        }
    }
}