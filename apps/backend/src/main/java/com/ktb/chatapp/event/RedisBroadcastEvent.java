package com.ktb.chatapp.event;

public record RedisBroadcastEvent(
    String roomId,
    String event,
    Object payload
) {
    public static RedisBroadcastEvent of(String roomId, String event, Object payload) {
        return new RedisBroadcastEvent(roomId, event, payload);
    }
}
