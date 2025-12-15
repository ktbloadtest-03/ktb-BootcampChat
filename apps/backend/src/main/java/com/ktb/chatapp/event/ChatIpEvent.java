package com.ktb.chatapp.event;

import com.ktb.chatapp.dto.MessageResponse;

public record ChatIpEvent(
    String roomId,
    String ip,
    com.ktb.chatapp.dto.MessageResponse messageResponse
) {
    public static ChatIpEvent of(String roomId, String ip, MessageResponse messageResponse) {
        return new ChatIpEvent(roomId, ip, messageResponse);
    }
}