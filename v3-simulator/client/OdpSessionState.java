// src/main/java/com/odp/simulator/client/session/OdpSessionState.java
package com.odp.simulator.client.session;

/**
 * Represents the state of an ODP session
 */
public enum OdpSessionState {
    
    /**
     * Initial state - not connected
     */
    DISCONNECTED,
    
    /**
     * Lookup request sent, waiting for response
     */
    LOOKUP_PENDING,
    
    /**
     * Lookup successful, gateway address obtained
     */
    LOOKUP_COMPLETE,
    
    /**
     * Connecting to trading gateway
     */
    CONNECTING,
    
    /**
     * Logon request sent, waiting for response
     */
    LOGON_PENDING,
    
    /**
     * Session is active and ready for trading
     */
    ACTIVE,
    
    /**
     * Session is being terminated
     */
    LOGGING_OUT,
    
    /**
     * Session terminated normally
     */
    LOGGED_OUT,
    
    /**
     * Session terminated due to error
     */
    ERROR
}