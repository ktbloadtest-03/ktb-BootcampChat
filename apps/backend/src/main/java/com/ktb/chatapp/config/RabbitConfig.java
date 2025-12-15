package com.ktb.chatapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitConfig {
    public static final String CHAT_MESSAGE_QUEUE = "chat.message.queue";
    public static final String CHAT_LEAVE_QUEUE = "chat.leave.queue";
    public static final String CHAT_MARK_QUEUE = "chat.mark.queue";
    public static final String CHAT_PARTICIPANTS_QUEUE = "chat.participants.queue";

    public static final String ROUTING_CHAT_MESSAGE = "chat.message";
    public static final String ROUTING_LEAVE_MESSAGE = "chat.leave";
    public static final String ROUTING_MARK_MESSAGE = "chat.mark";
    public static final String ROUTING_PARTICIPANTS = "chat.participants";

    public static final String EXCHANGE = "chat.exchange";

    @Bean
    public Queue chatMessageQueue() {
        return new Queue(CHAT_MESSAGE_QUEUE, true); // durable
    }

    @Bean
    public Queue chatLeaveQueue() {
        return new Queue(CHAT_LEAVE_QUEUE, true);
    }

    @Bean
    public Queue markAsReadQueue() {
        return new Queue(CHAT_MARK_QUEUE, true);
    }

    @Bean
    public Queue participantsQueue() {
        return new Queue(CHAT_PARTICIPANTS_QUEUE, true);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Binding messageBinding() {
        return BindingBuilder
            .bind(chatMessageQueue())
            .to(exchange())
            .with(ROUTING_CHAT_MESSAGE);
    }

    @Bean
    public Binding leaveBinding() {
        return BindingBuilder
            .bind(chatLeaveQueue())
            .to(exchange())
            .with(ROUTING_LEAVE_MESSAGE);
    }

    @Bean
    public Binding markAsReadBinding() {
        return BindingBuilder
            .bind(markAsReadQueue())
            .to(exchange())
            .with(ROUTING_MARK_MESSAGE);
    }

    @Bean
    public Binding participantsBinding() {
        return BindingBuilder
            .bind(participantsQueue())
            .to(exchange())
            .with(ROUTING_PARTICIPANTS);
    }

    @Bean
    public ObjectMapper rabbitObjectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    @Bean
    MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        // Enable publisher confirms/returns so we can detect routing failures.
        if (connectionFactory instanceof CachingConnectionFactory cachingConnectionFactory) {
            cachingConnectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
            cachingConnectionFactory.setPublisherReturns(true);
        }

        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true); // routing 실패 시 returnsCallback 실행
        template.setReturnsCallback(returned -> log.warn(
            "Rabbit return: replyCode={}, replyText={}, exchange={}, routingKey={}, message={}",
            returned.getReplyCode(), returned.getReplyText(), returned.getExchange(),
            returned.getRoutingKey(), returned.getMessage()));
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("Rabbit publish failed: correlationData={}, cause={}", correlationData, cause);
            }
        });
        return template;
    }
}
