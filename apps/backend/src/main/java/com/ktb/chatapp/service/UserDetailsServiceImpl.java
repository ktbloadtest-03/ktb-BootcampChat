package com.ktb.chatapp.service;

import com.ktb.chatapp.cache.UserCacheStore;
import com.ktb.chatapp.model.User;
import com.ktb.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserCacheStore userCacheStore;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//        User user = userRepository.findByEmail(email.toLowerCase())
//                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        User user = userCacheStore.getUserByEmail(email);

        // username은 email로 설정
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                new ArrayList<>()
        );
    }
}
