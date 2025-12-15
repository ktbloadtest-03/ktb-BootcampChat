package com.ktb.chatapp.websocket.socketio.handler;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.ktb.chatapp.cache.IpCacheStore;
import com.ktb.chatapp.cache.RoomCacheStore;
import com.ktb.chatapp.dto.MessageResponse;
import com.ktb.chatapp.dto.UserResponse;
import com.ktb.chatapp.event.RedisBroadcastEvent;
import com.ktb.chatapp.model.Message;
import com.ktb.chatapp.model.MessageType;
import com.ktb.chatapp.model.Room;
import com.ktb.chatapp.model.User;
import com.ktb.chatapp.rabbitmq.RabbitPublisher;
import com.ktb.chatapp.redis.ChatRedisPublisher;
import com.ktb.chatapp.repository.MessageRepository;
import com.ktb.chatapp.repository.RoomRepository;
import com.ktb.chatapp.repository.UserRepository;
import com.ktb.chatapp.websocket.socketio.SocketUser;
import com.ktb.chatapp.websocket.socketio.UserRooms;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import static com.ktb.chatapp.websocket.socketio.SocketIOEvents.*;

/**
 * 방 퇴장 처리 핸들러
 * 채팅방 퇴장, 스트리밍 세션 종료, 참가자 목록 업데이트 담당
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "socketio.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class RoomLeaveHandler {

    private final SocketIOServer socketIOServer;
    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final UserRooms userRooms;
    private final MessageResponseMapper messageResponseMapper;
    private final RoomCacheStore roomCacheStore;
    private final IpCacheStore ipCacheStore;
    private final RabbitPublisher rabbitPublisher;
    private final ChatRedisPublisher chatRedisPublisher;
    private final ApplicationEventPublisher eventPublisher;

    @OnEvent(LEAVE_ROOM)
    public void handleLeaveRoom(SocketIOClient client, String roomId) {
        try {
            String userId = getUserId(client);
            String userName = getUserName(client);

            if (userId == null) {
                client.sendEvent(ERROR, Map.of("message", "Unauthorized"));
                return;
            }

            if (!userRooms.isInRoom(userId, roomId)) {
                log.debug("User {} is not in room {}", userId, roomId);
                return;
            }

            User user = userRepository.findById(userId).orElse(null);
            Room room = roomRepository.findById(roomId).orElse(null);

            if (user == null || room == null) {
                log.warn("Room {} not found or user {} has no access", roomId, userId);
                return;
            }

            roomCacheStore.evictRoom(roomId);
            roomRepository.removeParticipant(roomId, userId);
            ipCacheStore.removeIp(userId);

            client.leaveRoom(roomId);
            userRooms.remove(userId, roomId);
            
            log.info("User {} left room {}", userName, room.getName());
            
            log.debug("Leave room cleanup - roomId: {}, userId: {}", roomId, userId);
            
            sendSystemMessage(roomId, userName + "님이 퇴장하였습니다.");
            broadcastParticipantList(roomId);
            Map<String, String> userInfo = Map.of(
                "userId", userId,
                "userName", userName
            );
            socketIOServer.getRoomOperations(roomId)
                    .sendEvent(USER_LEFT, userInfo);
//            chatRedisPublisher.publish(roomId, USER_LEFT, userInfo);
            eventPublisher.publishEvent(RedisBroadcastEvent.of(roomId, USER_LEFT, userInfo));
        } catch (Exception e) {
            log.error("Error handling leaveRoom", e);
            client.sendEvent(ERROR, Map.of("message", "채팅방 퇴장 중 오류가 발생했습니다."));
        }
    }
    
    private void sendSystemMessage(String roomId, String content) {
        try {
            Message systemMessage = new Message();
            systemMessage.setRoomId(roomId);
            systemMessage.setContent(content);
            systemMessage.setType(MessageType.system);
            systemMessage.setTimestamp(LocalDateTime.now());
            systemMessage.setMentions(new ArrayList<>());
            systemMessage.setIsDeleted(false);
            systemMessage.setReactions(new HashMap<>());
            systemMessage.setReaders(new ArrayList<>());
            systemMessage.setMetadata(new HashMap<>());

            Message savedMessage = messageRepository.save(systemMessage);
            MessageResponse response = messageResponseMapper.mapToMessageResponse(savedMessage, null);

            socketIOServer.getRoomOperations(roomId)
                    .sendEvent(MESSAGE, response);
//            chatRedisPublisher.publish(roomId, MESSAGE, response);
            eventPublisher.publishEvent(RedisBroadcastEvent.of(roomId, MESSAGE, response));
//            Room room = roomRepository.findById(roomId).orElse(null);
//            rabbitPublisher.sendChatEvent(roomId, new ArrayList<>(room.getParticipantIds()), response);
//            rabbitPublisher.sendMessage(new ArrayList<>(room.getParticipantIds()), response);

        } catch (Exception e) {
            log.error("Error sending system message", e);
        }
    }
    
    private void broadcastParticipantList(String roomId) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return;
        }

        var participantList = roomOpt.get()
                .getParticipantIds()
                .stream()
                .map(userRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(UserResponse::from)
                .toList();

        if (participantList.isEmpty()) {
            return;
        }
        
        socketIOServer.getRoomOperations(roomId)
                .sendEvent(PARTICIPANTS_UPDATE, participantList);
//        chatRedisPublisher.publish(roomId, PARTICIPANTS_UPDATE, participantList);
        eventPublisher.publishEvent(RedisBroadcastEvent.of(roomId, PARTICIPANTS_UPDATE, participantList));
    }

    private SocketUser getUserDto(SocketIOClient client) {
        return client.get("user");
    }

    private String getUserId(SocketIOClient client) {
        SocketUser user = getUserDto(client);
        return user != null ? user.id() : null;
    }

    private String getUserName(SocketIOClient client) {
        SocketUser user = getUserDto(client);
        return user != null ? user.name() : null;
    }
}
