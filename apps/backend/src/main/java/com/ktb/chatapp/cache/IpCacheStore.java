package com.ktb.chatapp.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IpCacheStore {

    @CachePut(value= "userIp", key= "#userId", cacheManager = "cacheManager" )
    public String saveIp(String userId, String ip) {
        return ip;
    }

    @Cacheable(value = "userIp", key="#userId", cacheManager = "cacheManager")
    public String getIp(String userId) {
        return "retry";
    }

    @CacheEvict(cacheNames = "userIp", key = "#userId", cacheManager = "cacheManager")
    public void removeIp(String userId) {

    }
}
