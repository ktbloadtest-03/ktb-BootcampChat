package com.ktb.chatapp.event;

import com.ktb.chatapp.dto.MessageResponse;

public record ChatMessageIpEvent(
    String ip,
    String userId,
    MessageResponse messageResponse

) {
    public static ChatMessageIpEvent of(String ip, String userId, MessageResponse messageResponse) {
        return new ChatMessageIpEvent(ip, userId, messageResponse);
    }
}
