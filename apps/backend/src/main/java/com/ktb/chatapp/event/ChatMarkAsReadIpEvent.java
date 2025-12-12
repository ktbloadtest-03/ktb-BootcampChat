package com.ktb.chatapp.event;

import com.ktb.chatapp.dto.MessagesReadResponse;

public record ChatMarkAsReadIpEvent(
    String ip,
    String userId,
    MessagesReadResponse messagesReadResponse
) {
    public static ChatMarkAsReadIpEvent of(String ip, String userId, MessagesReadResponse messagesReadResponse) {
        return new ChatMarkAsReadIpEvent(ip, userId, messagesReadResponse);
    }
}
