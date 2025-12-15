package com.ktb.chatapp.event;

import com.ktb.chatapp.dto.MessagesReadResponse;

public record ChatMarkAsReadIpEvent(
    String roomId,
    String ip,
    MessagesReadResponse messagesReadResponse
) {
    public static ChatMarkAsReadIpEvent of(String roomId, String ip, MessagesReadResponse messagesReadResponse) {
        return new ChatMarkAsReadIpEvent(roomId, ip, messagesReadResponse);
    }
}
