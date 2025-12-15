package com.ktb.chatapp.rabbitmq;

import com.ktb.chatapp.dto.MessageResponse;
import com.ktb.chatapp.dto.MessagesReadResponse;
import com.ktb.chatapp.dto.UserResponse;
import com.ktb.chatapp.event.ChatLeaveEvent;
import com.ktb.chatapp.event.ChatParticipantsEvent;
import com.ktb.chatapp.event.ChatEvent;
import com.ktb.chatapp.websocket.socketio.handler.MessageResponseMapper;
import com.ktb.chatapp.websocket.socketio.handler.mqevent.ChatJoinEvent;
import com.ktb.chatapp.websocket.socketio.handler.mqevent.ChatMarkAsReadEvent;
import com.ktb.chatapp.websocket.socketio.handler.mqevent.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.ktb.chatapp.config.RabbitConfig.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitPublisher {
    private static final int CHUNK_SIZE = 200;
    private final RabbitTemplate rabbitTemplate;
    private final MessageResponseMapper messageResponseMapper;

    // TODO: 일반, 입장, 퇴장 메시지 처리
    public void sendChatEvent(String roomId, List<String> participantIds, MessageResponse messageResponse) {
        log.info("handleChatMessage => {}",  messageResponse.getContent());
        ChatEvent chatEvent = ChatEvent.of(roomId, participantIds, messageResponse);
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_CHAT_MESSAGE, chatEvent);
    }

//    public void sendMessage(List<String> participantIds, MessageResponse messageResponse) {
//        log.info("send message {}", messageResponse.getContent());
//        for (int i = 0; i < participantIds.size(); i += CHUNK_SIZE) {
//            var chunk = participantIds.subList(i, Math.min(i + CHUNK_SIZE, participantIds.size()));
//            ChatMessageEvent chatMessageEvent = ChatMessageEvent.of(messageResponse, chunk);
//            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_CHAT_MESSAGE, chatMessageEvent);
//        }
//    }
//
//    public void joinRoom(List<String> participantIds, MessageResponse messageResponse) {
//        for (int i = 0; i < participantIds.size(); i += CHUNK_SIZE) {
//            var chunk = participantIds.subList(i, Math.min(i + CHUNK_SIZE, participantIds.size()));
//            ChatJoinEvent chatJoinEvent = ChatJoinEvent.of(messageResponse, chunk);
//            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_JOIN_MESSAGE, chatJoinEvent);
//        }
//    }

    public void markAsRead(String roomId, List<String> participantIds, MessagesReadResponse messagesReadResponse) {
        ChatMarkAsReadEvent chatMarkAsReadEvent = ChatMarkAsReadEvent.of(roomId, messagesReadResponse, participantIds);
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_MARK_MESSAGE, chatMarkAsReadEvent);
    }

    public void updateParticipants(List<String> participantIds, List<UserResponse> participants) {
        for (int i = 0; i < participantIds.size(); i += CHUNK_SIZE) {
            var chunk = participantIds.subList(i, Math.min(i + CHUNK_SIZE, participantIds.size()));
            ChatParticipantsEvent chatParticipantsEvent = ChatParticipantsEvent.of(participants, chunk);
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_PARTICIPANTS, chatParticipantsEvent);
        }
    }

    public void leaveRoom(List<String> participantIds, Map<String, String> userInfo) {
        for (int i = 0; i < participantIds.size(); i += CHUNK_SIZE) {
            var chunk = participantIds.subList(i, Math.min(i + CHUNK_SIZE, participantIds.size()));
            ChatLeaveEvent chatLeaveEvent = ChatLeaveEvent.of(chunk, userInfo);
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_LEAVE_MESSAGE, chatLeaveEvent);
        }
    }
}
