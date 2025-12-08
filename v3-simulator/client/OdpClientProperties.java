// src/main/java/com/odp/simulator/client/config/OdpClientProperties.java
package com.odp.simulator.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "odp.client")
public class OdpClientProperties {

    /**
     * Comp ID assigned to the client by HKEX
     */
    private String compId;

    /**
     * Password for the Comp ID
     */
    private String password;

    /**
     * EP Application Version ID certified by HKEX
     */
    private String epApplVersionId;

    /**
     * Heartbeat interval in seconds (1-60)
     */
    private int heartbeatIntervalSeconds = 3;

    /**
     * RSA public key path for password encryption
     */
    private String publicKeyPath;

    /**
     * Lookup service configuration
     */
    private LookupConfig lookup = new LookupConfig();

    /**
     * Trading connection configuration
     */
    private TradingConfig trading = new TradingConfig();

    @Data
    public static class LookupConfig {
        private EndpointConfig primarySitePrimary;
        private EndpointConfig primarySiteSecondary;
        private EndpointConfig secondarySitePrimary;
        private EndpointConfig secondarySiteSecondary;
        private long retryWaitMs = 5000;
        private int connectTimeoutMs = 10000;
    }

    @Data
    public static class TradingConfig {
        private int connectTimeoutMs = 30000;
        private int readTimeoutMs = 60000;
    }

    @Data
    public static class EndpointConfig {
        private String host;
        private int port;
    }
}