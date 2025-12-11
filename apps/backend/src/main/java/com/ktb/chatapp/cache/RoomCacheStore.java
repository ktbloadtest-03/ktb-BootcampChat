package com.ktb.chatapp.cache;

import com.ktb.chatapp.model.Room;
import com.ktb.chatapp.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomCacheStore {
    private final RoomRepository roomRepository;

    @Cacheable(value = "RoomCache", key="#roomId", cacheManager = "cacheManager")
    public Room getRoom(String roomId) {
        return roomRepository.findById(roomId).orElse(null);
    }

    @CacheEvict(cacheNames = "RoomCache", key = "#roomId", cacheManager = "cacheManager")
    public void evictRoom(String roomId) {

    }
}
