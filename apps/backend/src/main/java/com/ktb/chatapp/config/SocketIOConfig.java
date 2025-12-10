package com.ktb.chatapp.config;

import com.corundumstudio.socketio.AuthTokenListener;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.corundumstudio.socketio.store.MemoryStoreFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ktb.chatapp.websocket.socketio.ChatDataStore;
import com.ktb.chatapp.websocket.socketio.LocalChatDataStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Role;

import static org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "socketio.enabled", havingValue = "true", matchIfMissing = true)
public class SocketIOConfig {

    @Value("${socketio.server.host:localhost}")
    private String host;

    @Value("${socketio.server.port:5002}")
    private Integer port;

    @Bean(initMethod = "start", destroyMethod = "stop")
    public SocketIOServer socketIOServer(AuthTokenListener authTokenListener) {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(host);
        config.setPort(port);
        
        var socketConfig = new SocketConfig();
        socketConfig.setReuseAddress(true);
        socketConfig.setTcpNoDelay(false);
        socketConfig.setAcceptBackLog(10);
        socketConfig.setTcpSendBufferSize(4096);
        socketConfig.setTcpReceiveBufferSize(4096);
        config.setSocketConfig(socketConfig);

        config.setOrigin("*");

        // Socket.IO settings
        config.setPingTimeout(60000);
        config.setPingInterval(25000);
        config.setUpgradeTimeout(10000);

        config.setJsonSupport(new JacksonJsonSupport(new JavaTimeModule()));
        config.setStoreFactory(new MemoryStoreFactory()); // 단일노드 전용

        log.info("Socket.IO server configured on {}:{} with {} boss threads and {} worker threads",
                 host, port, config.getBossThreads(), config.getWorkerThreads());
        var socketIOServer = new SocketIOServer(config);
        socketIOServer.getNamespace(Namespace.DEFAULT_NAME).addAuthTokenListener(authTokenListener);
        
        return socketIOServer;
    }
    
    /**
     * SpringAnnotationScanner는 BeanPostProcessor로서
     * ApplicationContext 초기화 초기에 등록되고,
     * 내부에서 사용하는 SocketIOServer는 Lazy로 지연되어
     * 다른 Bean들의 초기화 과정에 간섭하지 않게 한다.
     */
    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    public BeanPostProcessor springAnnotationScanner(@Lazy SocketIOServer socketIOServer) {
        return new SpringAnnotationScanner(socketIOServer);
    }
    
    // 인메모리 저장소, 단일 노드 환경에서만 사용
    @Bean
    @ConditionalOnProperty(name = "socketio.enabled", havingValue = "true", matchIfMissing = true)
    public ChatDataStore chatDataStore() {
        return new LocalChatDataStore();
    }
    
    /**
     * Socket.IO 서버의 내부 Netty 서버에 헬스 체크 핸들러를 추가합니다.
     * 리플렉션을 사용하여 내부 구조에 접근합니다.
     */
    private void addHealthCheckHandler(SocketIOServer socketIOServer) {
        try {
            // SocketIOServer의 내부 bootstrap 필드에 접근
            Field bootstrapField = socketIOServer.getClass().getDeclaredField("bootstrap");
            bootstrapField.setAccessible(true);
            ServerBootstrap bootstrap = (ServerBootstrap) bootstrapField.get(socketIOServer);
            
            // Bootstrap의 childHandler에 헬스 체크 핸들러 추가
            if (bootstrap != null) {
                bootstrap.childHandler(new io.netty.channel.ChannelInitializer<io.netty.channel.socket.SocketChannel>() {
                    @Override
                    protected void initChannel(io.netty.channel.socket.SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 기존 핸들러들 앞에 헬스 체크 핸들러 추가
                        pipeline.addFirst("healthCheck", new SocketIOHealthCheckHandler());
                    }
                });
                log.info("Health check handler added to Socket.IO server");
            }
        } catch (NoSuchFieldException e) {
            log.warn("Could not find bootstrap field in SocketIOServer, trying alternative approach", e);
            // 대안: Socket.IO 서버가 시작된 후 ChannelPipeline에 직접 추가
            // 이 방법은 서버가 시작된 후에만 가능하므로 별도의 초기화 로직이 필요할 수 있습니다.
        } catch (Exception e) {
            log.warn("Failed to add health check handler", e);
        }
    }
}
