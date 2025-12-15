package com.ktb.chatapp.event;

import com.ktb.chatapp.dto.MessageResponse;

import java.util.List;

public record ChatEvent(
    String roomId,
    List<String> participantIds,
    MessageResponse messageResponse
) {
    public static ChatEvent of(String roomId, List<String> participantIds, MessageResponse messageResponse) {
        return new ChatEvent(roomId, participantIds, messageResponse);
    }
}
