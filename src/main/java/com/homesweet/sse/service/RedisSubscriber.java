package com.homesweet.sse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homesweet.sse.dto.NotificationMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber {

    private final ReactiveRedisMessageListenerContainer reactiveRedisMessageListenerContainer;
    private final ObjectMapper objectMapper;
    private final SseService sseService;

    @PostConstruct
    public void subscribeToNotifications() {
        reactiveRedisMessageListenerContainer
                .receive(ChannelTopic.of("notification:push"))
                .onBackpressureBuffer(1000,
                        dropped -> log.warn("Redis notification buffer overflow. Dropping message."),
                        BufferOverflowStrategy.DROP_OLDEST)
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(message -> {
                    try {
                        NotificationMessage notificationMessage = objectMapper.readValue(
                                message.getMessage(), NotificationMessage.class);

                        sseService.sendNotification(
                                notificationMessage.getUserId(),
                                notificationMessage.getData());
                    } catch (Exception e) {
                        // JsonProcessingException 등 checked 예외를 여기서 처리
                        log.error("Error parsing Redis message", e);
                    }
                })
                .onErrorContinue((error, obj) -> log.error("Error processing Redis message", error))
                .subscribe();
    }
}
