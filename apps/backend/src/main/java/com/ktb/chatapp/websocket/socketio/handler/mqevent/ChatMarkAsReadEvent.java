package com.ktb.chatapp.websocket.socketio.handler.mqevent;

import com.ktb.chatapp.dto.MessagesReadResponse;

import java.util.List;

public record ChatMarkAsReadEvent(
    String roomId,
    List<String> participants,
    MessagesReadResponse messagesReadResponse
) {
    public static ChatMarkAsReadEvent of(String roomId, MessagesReadResponse messagesReadResponse, List<String> participants) {
        return new ChatMarkAsReadEvent(roomId, participants, messagesReadResponse);
    }
}
