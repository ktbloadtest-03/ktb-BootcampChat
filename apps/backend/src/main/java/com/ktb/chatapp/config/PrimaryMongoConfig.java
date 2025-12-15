package com.ktb.chatapp.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Primary
@Configuration
@EnableMongoRepositories(
    basePackages = "com.ktb.chatapp.repository",
    mongoTemplateRef = "primaryMongoTemplate"
)
public class PrimaryMongoConfig {
    @Value("${spring.data.mongodb.primary.uri}")
    private String uri;

    @Bean
    @Primary
    public MongoClient primaryMongoClient() {
        return MongoClients.create(uri);
    }

    @Bean
    @Primary
    public MongoDatabaseFactory primaryMongoDbFactory() {
        return new SimpleMongoClientDatabaseFactory(uri);
    }

    @Bean
    @Primary
    public MongoTemplate primaryMongoTemplate() {
        return new MongoTemplate(primaryMongoDbFactory());
    }
}
