package com.ktb.chatapp.controller;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.ktb.chatapp.event.*;
import com.ktb.chatapp.websocket.socketio.ConnectedUsers;
import com.ktb.chatapp.websocket.socketio.SocketUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.ktb.chatapp.websocket.socketio.SocketIOEvents.*;

@Slf4j
@RestController
@RequestMapping("/internal/socket")
@RequiredArgsConstructor
public class SocketDeliveryController {

    private final SocketIOServer socketIOServer;
    private final ConnectedUsers connectedUsers;

//    @PostMapping("/new")
//    public void createNewMessage(@RequestBody ChatMessageIpEvent event) {
//        log.info("Consumer REST API called - returning new message {}", event.messageResponse().getContent());
//        SocketUser socketUser = connectedUsers.get(event.userId());
//        SocketIOClient client = socketIOServer.getClient(UUID.fromString(socketUser.socketId()));
//        client.sendEvent(MESSAGE, event.messageResponse());
//    }

    @PostMapping("/new")
    public void createNewMessage(@RequestBody ChatEvent event) {
        log.info("Consumer returning new message {}", event.messageResponse().getContent());
        socketIOServer.getRoomOperations(event.roomId()).sendEvent(MESSAGE, event.messageResponse());
    }

    @PostMapping("/mark")
    public void mark(@RequestBody ChatMarkAsReadIpEvent event) {
        log.info("Consumer mark as read {}", event.messagesReadResponse().getUserId());
        socketIOServer.getRoomOperations(event.roomId()).sendEvent(MESSAGES_READ, event.messagesReadResponse());
    }

//    @PostMapping("/join")
//    public void joinRoom(@RequestBody ChatJoinIpEvent event) {
//        log.info("Consumer REST API called - join {}", event.messageResponse().getContent());
//        SocketUser socketUser = connectedUsers.get(event.userId());
//        SocketIOClient client = socketIOServer.getClient(UUID.fromString(socketUser.socketId()));
//        client.sendEvent(MESSAGE, event.messageResponse());
//    }

    @PostMapping("/join")
    public void joinRoom(@RequestBody ChatEvent event) {
        log.info("Consumer REST API called - join {}", event.messageResponse().getContent());
        socketIOServer.getRoomOperations(event.roomId()).sendEvent(MESSAGE, event.messageResponse());
    }

    @PostMapping("/participants")
    public void joinRoom(@RequestBody ChatParticipantsIpEvent event) {
        log.info("Consumer REST API called - participants");
        SocketUser socketUser = connectedUsers.get(event.userId());
        SocketIOClient client = socketIOServer.getClient(UUID.fromString(socketUser.socketId()));
        client.sendEvent(PARTICIPANTS_UPDATE, event.participants());
    }

    @PostMapping("/leave")
    public void leaveRoom(@RequestBody ChatLeaveIpEvent event) {
        log.info("Consumer REST API called - leave");
        SocketUser socketUser = connectedUsers.get(event.userId());
        SocketIOClient client = socketIOServer.getClient(UUID.fromString(socketUser.socketId()));
        client.sendEvent(USER_LEFT, event.userInfo());
    }

    @PostMapping("/created")
    public void roomCreated(@RequestBody RoomCreatedEvent event) {
        log.info("Consumer REST API called - created");
        socketIOServer.getRoomOperations("room-list").sendEvent(ROOM_CREATED, event.getRoomResponse());
    }

    @PostMapping("/updated")
    public void roomUpdated(@RequestBody RoomUpdatedEvent event) {
        log.info("Consumer REST API called - updated");
        socketIOServer.getRoomOperations(event.getRoomId()).sendEvent(ROOM_UPDATE, event.getRoomResponse());
    }
}
