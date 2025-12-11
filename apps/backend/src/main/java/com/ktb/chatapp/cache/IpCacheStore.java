package com.ktb.chatapp.cache;

import com.ktb.chatapp.model.Room;
import com.ktb.chatapp.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IpCacheStore {
    private final IpExtractor ipExtractor;

    @Cacheable(value = "userIp", key="#userId", cacheManager = "cacheManager")
    public String getIp(String userId) {
        return ipExtractor.getIpFromHeader();
    }

    @CacheEvict(cacheNames = "userIp", key = "#userId", cacheManager = "cacheManager")
    public void removeIp(String userId) {

    }
}
