package com.ktb.chatapp.event;

import java.util.List;
import java.util.Map;

public record ChatLeaveEvent(
    List<String> participants,
    Map<String, String> userInfo
) {
    public static ChatLeaveEvent of(List<String> participants, Map<String, String> userInfo) {
        return new ChatLeaveEvent(participants, userInfo);
    }
}
