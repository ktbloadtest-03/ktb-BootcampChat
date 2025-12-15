package com.ktb.chatapp.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb.chatapp.redis.message.ChatBroadcastMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import static com.ktb.chatapp.config.RedisConfig.CHANNEL;
import static com.ktb.chatapp.config.RedisConfig.SERVER_ID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRedisPublisher {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(String roomId, String event, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            ChatBroadcastMessage msg = new ChatBroadcastMessage(SERVER_ID, roomId, event, payloadJson);
            stringRedisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(msg));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
