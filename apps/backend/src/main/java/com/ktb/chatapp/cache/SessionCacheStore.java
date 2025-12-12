package com.ktb.chatapp.cache;

import com.ktb.chatapp.model.Session;
import com.ktb.chatapp.repository.SessionRepository;
import jakarta.websocket.SessionException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionCacheStore {
    private final SessionRepository sessionRepository;

    @Cacheable(value = "user::session", key="#userId", cacheManager = "cacheManager")
    public Session getSession(String userId) {
        return sessionRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("session_not_found"));
    }

    @CacheEvict(value = "user::session", key="#userId", cacheManager = "cacheManager")
    public void evictSession(String userId) {
        sessionRepository.deleteByUserId(userId);
    }
}
