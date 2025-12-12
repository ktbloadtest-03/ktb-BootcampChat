package com.ktb.chatapp.service.session;

import com.ktb.chatapp.cache.SessionCacheStore;
import com.ktb.chatapp.model.Session;
import com.ktb.chatapp.repository.SessionRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * MongoDB implementation of SessionStore.
 * Uses SessionRepository for persistence.
 */
@Component
@RequiredArgsConstructor
public class SessionMongoStore implements SessionStore {
    private final SessionRepository sessionRepository;
    private final SessionCacheStore sessionCacheStore;
    
    @Override
    public Optional<Session> findByUserId(String userId) {
        return Optional.ofNullable(sessionCacheStore.getSession(userId));
    }
    
    @Override
    public Session save(Session session) {
        return sessionRepository.save(session);
    }
    
    @Override
    public void delete(String userId, String sessionId) {
        Session session = sessionCacheStore.getSession(userId);
        if (session != null && sessionId.equals(session.getSessionId())) {
            sessionCacheStore.evictSession(userId);
        }
    }
    
    @Override
    public void deleteAll(String userId) {
        sessionCacheStore.evictSession(userId);
    }
}
