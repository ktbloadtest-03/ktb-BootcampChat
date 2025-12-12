package com.ktb.chatapp.event;

import com.ktb.chatapp.dto.UserResponse;

import java.util.List;

public record ChatParticipantsEvent(
    List<String> participantIds,
    List<UserResponse> participants
) {
    public static ChatParticipantsEvent of(List<UserResponse> participants, List<String> participantIds) {
        return new ChatParticipantsEvent(participantIds, participants);
    }
}
