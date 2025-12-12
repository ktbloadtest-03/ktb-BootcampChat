package com.ktb.chatapp.websocket.socketio.handler.mqevent;

import com.ktb.chatapp.dto.MessageResponse;

import java.util.List;

public record ChatMessageEvent (
   List<String> participants,
   MessageResponse messageResponse
) {
    public static ChatMessageEvent of(MessageResponse messageResponse, List<String> participants) {
        return new ChatMessageEvent(participants, messageResponse);
    }
}
