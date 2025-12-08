// src/main/java/com/odp/simulator/client/session/OdpSessionManager.java
package com.odp.simulator.client.session;

import com.odp.simulator.client.config.OdpClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages ODP sessions
 * 
 * In a production system, there might be multiple sessions (Comp IDs)
 * managed by a single client application.
 */
@Slf4j
@Component
public class OdpSessionManager {

    private final OdpClientProperties properties;
    private final ConcurrentHashMap<String, OdpSession> sessions = new ConcurrentHashMap<>();
    private volatile OdpSession primarySession;

    public OdpSessionManager(OdpClientProperties properties) {
        this.properties = properties;
    }

    /**
     * Get or create the primary session for the configured Comp ID
     */
    public OdpSession getPrimarySession() {
        if (primarySession == null) {
            synchronized (this) {
                if (primarySession == null) {
                    primarySession = createSession(properties.getCompId());
                }
            }
        }
        return primarySession;
    }

    /**
     * Create a new session for a Comp ID
     */
    public OdpSession createSession(String compId) {
        OdpSession session = new OdpSession(compId);
        session.setHeartbeatIntervalSeconds(properties.getHeartbeatIntervalSeconds());
        sessions.put(compId, session);
        log.info("Created session for Comp ID: {}", compId);
        return session;
    }

    /**
     * Get session by Comp ID
     */
    public OdpSession getSession(String compId) {
        return sessions.get(compId);
    }

    /**
     * Remove session
     */
    public void removeSession(String compId) {
        OdpSession session = sessions.remove(compId);
        if (session != null) {
            log.info("Removed session for Comp ID: {}", compId);
        }
    }

    /**
     * Close all sessions
     */
    public void closeAllSessions() {
        sessions.values().forEach(session -> {
            if (session.isConnected()) {
                session.getChannel().close();
            }
            session.transitionTo(OdpSessionState.LOGGED_OUT);
        });
        sessions.clear();
        primarySession = null;
        log.info("All sessions closed");
    }
}