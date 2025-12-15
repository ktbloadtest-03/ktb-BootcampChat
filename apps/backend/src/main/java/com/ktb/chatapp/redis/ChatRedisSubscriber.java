package com.ktb.chatapp.redis;

import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb.chatapp.dto.MessageResponse;
import com.ktb.chatapp.dto.MessagesReadResponse;
import com.ktb.chatapp.dto.RoomResponse;
import com.ktb.chatapp.redis.message.ChatBroadcastMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.ktb.chatapp.config.RedisConfig.SERVER_ID;
import static com.ktb.chatapp.websocket.socketio.SocketIOEvents.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRedisSubscriber {
    private final SocketIOServer socketIOServer;
    private final ObjectMapper objectMapper;

    public void onMessage(String message) {
        try {
            ChatBroadcastMessage msg = objectMapper.readValue(message, ChatBroadcastMessage.class);
            if (SERVER_ID.equals(msg.serverId())) return;
            Object payload = objectMapper.readValue(msg.payloadJson(), getClassByMessageType(msg.event())); // 구조를 모르면 Object, 알면 DTO로 변환
            socketIOServer.getRoomOperations(msg.roomId()).sendEvent(msg.event(), payload);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private Class<?> getClassByMessageType(String type) {
        return switch (type) {
            case MESSAGE -> MessageResponse.class;
            case MESSAGES_READ -> MessagesReadResponse.class;
            case ROOM_CREATED, ROOM_UPDATE -> RoomResponse.class;
            case PARTICIPANTS_UPDATE -> List.class;
            case USER_LEFT, SESSION_ENDED -> Map.class;
            default -> null;
        };
    }
}
