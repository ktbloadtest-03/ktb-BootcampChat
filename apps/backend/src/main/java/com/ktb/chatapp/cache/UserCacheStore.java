package com.ktb.chatapp.cache;

import com.ktb.chatapp.model.User;
import com.ktb.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCacheStore {
    private final UserRepository userRepository;

    @Cacheable(value = "user::email", key = "#email", cacheManager = "cacheManager")
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase())
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }

    @CacheEvict(value = "user::email", key = "#email", cacheManager = "cacheManager")
    public void evictUserByEmail(String email) {}
}
