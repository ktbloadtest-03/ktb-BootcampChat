package com.ktb.chatapp.event;

import com.ktb.chatapp.dto.UserResponse;

import java.util.List;

public record ChatParticipantsIpEvent(
    String ip,
    String userId,
    List<UserResponse> participants
) {
    public static ChatParticipantsIpEvent of(String ip, String userId, List<UserResponse> participants) {
        return new ChatParticipantsIpEvent(ip, userId, participants);
    }
}
