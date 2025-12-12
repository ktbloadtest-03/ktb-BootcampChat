package com.ktb.chatapp.event;

import com.ktb.chatapp.dto.MessageResponse;

public record ChatJoinIpEvent(
    String ip,
    String userId,
    MessageResponse messageResponse
) {
    public static ChatJoinIpEvent of(String ip, String userId, MessageResponse messageResponse) {
        return new ChatJoinIpEvent(ip, userId, messageResponse);
    }
}
