package com.ktb.chatapp.rabbitmq;

import com.ktb.chatapp.config.RabbitConfig;
import com.ktb.chatapp.dto.MessageResponse;
import com.ktb.chatapp.websocket.socketio.handler.mqevent.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RabbitPublisher {
    private static final int CHUNK_SIZE = 200;
    private final RabbitTemplate rabbitTemplate;

    public void sendMessage(List<String> participantIds, MessageResponse messageResponse) {
        for (int i = 0; i < participantIds.size(); i += CHUNK_SIZE) {
            var chunk = participantIds.subList(i, Math.min(i + CHUNK_SIZE, participantIds.size()));
            ChatMessageEvent chatMessageEvent = new ChatMessageEvent(messageResponse, chunk);
            rabbitTemplate.convertAndSend(RabbitConfig.CHAT_MESSAGE_QUEUE, chatMessageEvent);
        }
    }
}
