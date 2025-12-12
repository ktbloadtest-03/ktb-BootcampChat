package com.ktb.chatapp.websocket.socketio.handler.mqevent;

import com.ktb.chatapp.dto.MessagesReadResponse;

import java.util.List;

public record ChatMarkAsReadEvent(
    List<String> participants,
    MessagesReadResponse messagesReadResponse
) {
    public static ChatMarkAsReadEvent of(MessagesReadResponse messagesReadResponse, List<String> participants) {
        return new ChatMarkAsReadEvent(participants, messagesReadResponse);
    }
}
