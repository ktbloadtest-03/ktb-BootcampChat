package com.ktb.chatapp.redis.message;

public record ChatBroadcastMessage(
    String serverId,
    String roomId,
    String event,
    String payloadJson
) { }
