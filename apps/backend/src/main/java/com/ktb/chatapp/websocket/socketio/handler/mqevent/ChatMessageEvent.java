package com.ktb.chatapp.websocket.socketio.handler.mqevent;

import com.ktb.chatapp.dto.MessageResponse;

import java.util.List;

public class ChatMessageEvent {
    private List<String> participants;
    private MessageResponse messageResponse;

    public ChatMessageEvent(MessageResponse messageResponse, List<String> participants) {
        this.messageResponse = messageResponse;
        this.participants = participants;
    }
}
