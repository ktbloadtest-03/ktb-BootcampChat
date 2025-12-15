package com.ktb.chatapp.websocket.socketio.handler;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.ktb.chatapp.cache.RoomCacheStore;
import com.ktb.chatapp.dto.*;
import com.ktb.chatapp.event.RedisBroadcastEvent;
import com.ktb.chatapp.model.Message;
import com.ktb.chatapp.model.MessageType;
import com.ktb.chatapp.model.Room;
import com.ktb.chatapp.rabbitmq.RabbitPublisher;
import com.ktb.chatapp.redis.ChatRedisPublisher;
import com.ktb.chatapp.repository.MessageRepository;
import com.ktb.chatapp.repository.RoomRepository;
import com.ktb.chatapp.repository.UserRepository;
import com.ktb.chatapp.websocket.socketio.SocketUser;
import com.ktb.chatapp.websocket.socketio.UserRooms;

import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import static com.ktb.chatapp.websocket.socketio.SocketIOEvents.*;

/**
 * 방 입장 처리 핸들러
 * 채팅방 입장, 참가자 관리, 초기 메시지 로드 담당
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "socketio.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class RoomJoinHandler {

    private final ChatRedisPublisher chatRedisPublisher;
    @Value("${server_ip}")
    private String serverIp;

    private final SocketIOServer socketIOServer;
    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final UserRooms userRooms;
    private final MessageLoader messageLoader;
    private final MessageResponseMapper messageResponseMapper;
    private final RoomLeaveHandler roomLeaveHandler;
    private final RoomCacheStore roomCacheStore;

    private final IpCacheStore ipCacheStore;
    private final RabbitPublisher rabbitPublisher;
    private final ApplicationEventPublisher eventPublisher;


    @OnEvent(JOIN_ROOM)
    public void handleJoinRoom(SocketIOClient client, String roomId) {
        try {
            String userId = getUserId(client);
            String userName = getUserName(client);

            if (userId == null) {
                log.warn("Join room failed: Unauthorized - roomId: {}", roomId);
                client.sendEvent(JOIN_ROOM_ERROR, Map.of("message", "Unauthorized"));
                return;
            }

            if (userRepository.findById(userId).isEmpty()) {
                log.warn("Join room failed: User not found - userId: {}, roomId: {}", userId, roomId);
                client.sendEvent(JOIN_ROOM_ERROR, Map.of("message", "User not found"));
                return;
            }

            // Room 조회 시 재시도 로직 추가 (MongoDB eventual consistency 대응)
            Optional<Room> roomOpt = findRoomWithRetry(roomId, 3, 100);
            if (roomOpt.isEmpty()) {
                log.error("Join room failed: Room not found after retries - userId: {}, roomId: {}", userId, roomId);
                client.sendEvent(JOIN_ROOM_ERROR, Map.of("message", "채팅방을 찾을 수 없습니다."));
                return;
            }

            // 이미 해당 방에 참여 중인지 확인
            if (userRooms.isInRoom(userId, roomId)) {
                log.debug("User {} already in room {}", userId, roomId);
                client.joinRoom(roomId);
                client.sendEvent(JOIN_ROOM_SUCCESS, Map.of("roomId", roomId));
                return;
            }

            // MongoDB의 $addToSet 연산자를 사용한 원자적 업데이트
            roomRepository.addParticipant(roomId, userId);
            roomCacheStore.evictRoom(roomId);

            // Join socket room and add to user's room set
            client.joinRoom(roomId);
            userRooms.add(userId, roomId);
            ipCacheStore.saveIp(userId, serverIp);

            Message joinMessage = Message.builder()
                    .roomId(roomId)
                    .content(userName + "님이 입장하였습니다.")
                    .type(MessageType.system)
                    .timestamp(LocalDateTime.now())
                    .mentions(new ArrayList<>())
                    .isDeleted(false)
                    .reactions(new HashMap<>())
                    .readers(new ArrayList<>())
                    .metadata(new HashMap<>())
                    .build();

            joinMessage = messageRepository.save(joinMessage);

            // 초기 메시지 로드
            FetchMessagesRequest req = new FetchMessagesRequest(roomId, 30, null);
            FetchMessagesResponse messageLoadResult = messageLoader.loadMessages(req, userId);

            // 업데이트된 room 다시 조회하여 최신 participantIds 가져오기 (재시도 포함)
            roomOpt = findRoomWithRetry(roomId, 3, 100);
            if (roomOpt.isEmpty()) {
                log.error("Join room failed: Room not found after participant update - userId: {}, roomId: {}", userId, roomId);
                client.sendEvent(JOIN_ROOM_ERROR, Map.of("message", "채팅방을 찾을 수 없습니다."));
                return;
            }

            // 참가자 정보 조회
            List<UserResponse> participants = roomOpt.get().getParticipantIds()
                    .stream()
                    .map(userRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(UserResponse::from)
                    .toList();

            JoinRoomSuccessResponse response = JoinRoomSuccessResponse.builder()
                    .roomId(roomId)
                    .participants(participants)
                    .messages(messageLoadResult.getMessages())
                    .hasMore(messageLoadResult.isHasMore())
                    .activeStreams(Collections.emptyList())
                    .build();

            client.sendEvent(JOIN_ROOM_SUCCESS, response);

            // 입장 메시지 브로드캐스트
            MessageResponse messageResponse = messageResponseMapper.mapToMessageResponse(joinMessage, null);
            socketIOServer.getRoomOperations(roomId)
               .sendEvent(MESSAGE, messageResponse);
//            chatRedisPublisher.publish(roomId, MESSAGE, messageResponse);
            eventPublisher.publishEvent(RedisBroadcastEvent.of(roomId, MESSAGE, messageResponse));

            // 참가자 목록 업데이트 브로드캐스트
            socketIOServer.getRoomOperations(roomId)
                .sendEvent(PARTICIPANTS_UPDATE, participants);
//            chatRedisPublisher.publish(roomId, PARTICIPANTS_UPDATE, participants);
            eventPublisher.publishEvent(RedisBroadcastEvent.of(roomId, PARTICIPANTS_UPDATE, participants));

            log.info("User {} joined room {} successfully. Message count: {}, hasMore: {}",
                    userName, roomId, messageLoadResult.getMessages().size(), messageLoadResult.isHasMore());

        } catch (Exception e) {
            log.error("Error handling joinRoom", e);
            client.sendEvent(JOIN_ROOM_ERROR, Map.of(
                    "message", e.getMessage() != null ? e.getMessage() : "채팅방 입장에 실패했습니다."
            ));
        }
    }

    private SocketUser getUser(SocketIOClient client) {
        return client.get("user");
    }

    private String getUserId(SocketIOClient client) {
        SocketUser user = getUser(client);
        return user != null ? user.id() : null;
    }

    private String getUserName(SocketIOClient client) {
        SocketUser user = getUser(client);
        return user != null ? user.name() : null;
    }

    /**
     * Room 조회 시 재시도 로직
     * MongoDB eventual consistency 문제로 인해 방금 생성된 room이 즉시 조회되지 않을 수 있음
     *
     * @param roomId 조회할 room ID
     * @param maxRetries 최대 재시도 횟수
     * @param retryDelayMs 재시도 간격 (밀리초)
     * @return Room Optional
     */
    private Optional<Room> findRoomWithRetry(String roomId, int maxRetries, long retryDelayMs) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            Optional<Room> roomOpt = roomRepository.findById(roomId);
            if (roomOpt.isPresent()) {
                if (attempt > 1) {
                    log.info("Room found after {} retries - roomId: {}", attempt, roomId);
                }
                return roomOpt;
            }

            if (attempt < maxRetries) {
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Room lookup retry interrupted - roomId: {}", roomId);
                    break;
                }
                log.debug("Room not found, retrying ({}/{}) - roomId: {}", attempt, maxRetries, roomId);
            }
        }

        return Optional.empty();
    }
}