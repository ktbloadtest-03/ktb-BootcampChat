package com.ktb.chatapp.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(
    basePackages = "com.ktb.chatapp.message.repository", // Message 리포지토리를 이 패키지로 분리
    mongoTemplateRef = "messageMongoTemplate")
public class MessageMongoConfig {
    @Value("${spring.data.mongodb.message.uri}")
    private String uri;

    @Bean
    public MongoClient messageMongoClient() {
        return MongoClients.create(uri);
    }

    @Bean
    public MongoDatabaseFactory messageMongoDbFactory() {
        return new SimpleMongoClientDatabaseFactory(uri);
    }

    @Bean
    public MongoTemplate messageMongoTemplate() {
        return new MongoTemplate(messageMongoDbFactory());
    }
}
