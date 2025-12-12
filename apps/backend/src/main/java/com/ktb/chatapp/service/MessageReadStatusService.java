package com.ktb.chatapp.service;

import com.ktb.chatapp.model.Message;
import com.ktb.chatapp.repository.MessageRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * 메시지 읽음 상태 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageReadStatusService {

    private final MessageRepository messageRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * 메시지 읽음 상태 업데이트
     *
     * @param messageIds 읽음 상태를 업데이트할 메시지 리스트
     * @param userId 읽은 사용자 ID
     */
    public void updateReadStatus(List<String> messageIds, String userId) {
        if (messageIds.isEmpty()) {
            return;
        }
        
        Message.MessageReader readerInfo = Message.MessageReader.builder()
                .userId(userId)
                .readAt(LocalDateTime.now())
                .build();
        
        try {
            Query query = new Query(Criteria.where("id").in(messageIds)
                .and("readers.userId").ne(userId));
            Update update = new Update().addToSet("readers").each(readerInfo);
            mongoTemplate.updateMulti(query, update, Message.class);

//            List<Message> messages = new ArrayList<>();
//            List<Message> messagesToUpdate = messageRepository.findAllById(messageIds);
//            for (Message message : messagesToUpdate) {
//                if (message.getReaders() == null) {
//                    message.setReaders(new ArrayList<>());
//                }
//                boolean alreadyRead = message.getReaders().stream()
//                        .anyMatch(r -> r.getUserId().equals(userId));
//                if (!alreadyRead) {
//                    message.getReaders().add(readerInfo);
//                }
//                messages.add(message);
//            }
//            messageRepository.saveAll(messages);

//            log.debug("Read status updated for {} messages by user {}",
//                    messagesToUpdate.size(), userId);

        } catch (Exception e) {
            log.error("Read status update error for user {}", userId, e);
        }
    }
}
