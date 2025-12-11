package com.ktb.chatapp.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.api.StatefulConnection;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@EnableRedisRepositories
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public RedisConnectionFactory redisConnectionFactory(){
//        RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration(host, port);
//        return new LettuceConnectionFactory(standaloneConfiguration);
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(host, port);

        GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(16);      // 최대 커넥션 수
        poolConfig.setMaxIdle(8);        // 최대 idle
        poolConfig.setMinIdle(2);        // 최소 idle
        poolConfig.setMaxWait(Duration.ofSeconds(5)); // 풀 고갈 시 대기 시간
        poolConfig.setLifo(false);

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
            .poolConfig(poolConfig)
            .commandTimeout(Duration.ofSeconds(3))
            .build();

        return new LettuceConnectionFactory(standalone, clientConfig);
    }

    @Bean
    public RedisTemplate<String,String> redisTemplate(){
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer()); // 키-밸류 직렬화 방식은 프로젝트에 맞게 설정하세요
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        return redisTemplate;
    }

    @Bean(name = "cacheManager")
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator
            .builder()
            .allowIfSubType(Object.class)  // Object 클래스의 하위 타입을 허용
            .build();

        // 객체와 JSON 간의 변환을 관리
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());  // Time API 지원
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);  // 알려지지 않은 프로퍼티(필드)가 있을 때 실패하지 않고 무시
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL); // 기본 타이핑 활성화 및 다형성 타입 검증 설정
        objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);  // enum을 문자열로 쓰기
        objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);  // 문자열을 enum으로 읽기
        GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);  // 직렬화에 사용할 Serializer

        // 캐시의 기본 설정
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()  // null 캐싱X
            .entryTtl(Duration.ofHours(1L))
            .computePrefixWith(CacheKeyPrefix.simple())  // 캐시 키의 접두어를 간단하게 계산 EX) UserCache::Key 의 형태로 저장
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))  // 키의 직렬화 방식 설정
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(genericJackson2JsonRedisSerializer));  // 값의 직렬화 방식 설정

        // 캐시 이름 설정 담아주기
        Map<String, RedisCacheConfiguration> redisCacheConfigurationMap = new HashMap<>();
        redisCacheConfigurationMap.put("user::email", redisCacheConfiguration);
        redisCacheConfigurationMap.put("RoomCache", redisCacheConfiguration);

        // RedisCacheManager 리턴
        return RedisCacheManager.RedisCacheManagerBuilder
            .fromConnectionFactory(redisConnectionFactory)  // Redis 연결 설정
            .cacheDefaults(redisCacheConfiguration)  // 기본 캐시 설정
            .withInitialCacheConfigurations(redisCacheConfigurationMap)  // 초기 캐시 설정을 맵으로 전달
            .enableStatistics()
            .build();  // RedisCacheManager 생성
    }
}
