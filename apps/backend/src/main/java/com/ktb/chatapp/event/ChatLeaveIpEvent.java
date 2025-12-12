package com.ktb.chatapp.event;

import java.util.Map;

public record ChatLeaveIpEvent(
    String ip,
    String userId,
    Map<String,String> userInfo
) {
    public static ChatLeaveIpEvent of(String ip, String userId, Map<String, String> userInfo) {
        return new ChatLeaveIpEvent(ip, userId, userInfo);
    }
}
