package com.ktb.chatapp.websocket.socketio.handler.mqevent;

import com.ktb.chatapp.dto.MessageResponse;

import java.util.List;

public record ChatJoinEvent(
    List<String> participants,
    MessageResponse messageResponse
) {
    public static ChatJoinEvent of(MessageResponse messageResponse, List<String> participantIds) {
        return new ChatJoinEvent(participantIds, messageResponse);
    }
}
