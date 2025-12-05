package com.homesweet.sse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homesweet.sse.dto.NotificationMessage;
import com.homesweet.sse.dto.PushNotificationDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("RedisSubscriber 테스트")
class RedisSubscriberTest {

    private ReactiveRedisMessageListenerContainer container;
    private ObjectMapper objectMapper;
    private SseService sseService;
    private RedisSubscriber redisSubscriber;

    @BeforeEach
    void setUp() {
        container = mock(ReactiveRedisMessageListenerContainer.class);
        objectMapper = new ObjectMapper();
        sseService = mock(SseService.class);
        redisSubscriber = new RedisSubscriber(container, objectMapper, sseService);
    }

    @Test
    @DisplayName("Redis 메시지 수신 및 알림 전송 성공")
    void subscribeToNotifications_ShouldProcessMessageAndSendNotification() throws Exception {
        // given
        Long userId = 1L;
        PushNotificationDTO notification = PushNotificationDTO.builder()
                .notificationId(1L)
                .title("테스트 알림")
                .content("테스트 내용")
                .build();

        NotificationMessage notificationMessage = new NotificationMessage(userId, notification);
        String messageBody = objectMapper.writeValueAsString(notificationMessage);

        ReactiveSubscription.Message<String, String> redisMessage = mock(ReactiveSubscription.Message.class);
        when(redisMessage.getMessage()).thenReturn(messageBody);

        when(container.receive(any(ChannelTopic.class)))
                .thenReturn(Flux.just(redisMessage));

        doNothing().when(sseService).sendNotification(any(), any());

        // when
        redisSubscriber.subscribeToNotifications();

        // then - 메시지가 처리될 때까지 대기
        Thread.sleep(100);
        verify(sseService, timeout(1000).atLeastOnce()).sendNotification(eq(userId), any(PushNotificationDTO.class));
    }

    @Test
    @DisplayName("잘못된 JSON 메시지 처리 - 에러 무시")
    void subscribeToNotifications_WithInvalidJson_ShouldIgnoreError() {
        // given
        String invalidJson = "invalid json";

        ReactiveSubscription.Message<String, String> redisMessage = mock(ReactiveSubscription.Message.class);
        when(redisMessage.getMessage()).thenReturn(invalidJson);

        when(container.receive(any(ChannelTopic.class)))
                .thenReturn(Flux.just(redisMessage));

        // when
        redisSubscriber.subscribeToNotifications();

        // then - 에러가 발생해도 서비스가 계속 실행되어야 함
        verify(sseService, timeout(1000).never()).sendNotification(any(), any());
    }

    @Test
    @DisplayName("빈 메시지 처리")
    void subscribeToNotifications_WithEmptyMessage_ShouldHandleGracefully() {
        // given
        String emptyJson = "{}";

        ReactiveSubscription.Message<String, String> redisMessage = mock(ReactiveSubscription.Message.class);
        when(redisMessage.getMessage()).thenReturn(emptyJson);

        when(container.receive(any(ChannelTopic.class)))
                .thenReturn(Flux.just(redisMessage));

        // when
        redisSubscriber.subscribeToNotifications();

        // then - 에러가 발생하지 않아야 함
        verify(sseService, timeout(1000).never()).sendNotification(any(), any());
    }

    @Test
    @DisplayName("여러 메시지 순차 처리")
    void subscribeToNotifications_WithMultipleMessages_ShouldProcessAll() throws Exception {
        // given
        Long userId1 = 1L;
        Long userId2 = 2L;

        PushNotificationDTO notification1 = PushNotificationDTO.builder()
                .notificationId(1L)
                .title("알림1")
                .build();
        PushNotificationDTO notification2 = PushNotificationDTO.builder()
                .notificationId(2L)
                .title("알림2")
                .build();

        NotificationMessage message1 = new NotificationMessage(userId1, notification1);
        NotificationMessage message2 = new NotificationMessage(userId2, notification2);

        String json1 = objectMapper.writeValueAsString(message1);
        String json2 = objectMapper.writeValueAsString(message2);

        ReactiveSubscription.Message<String, String> redisMessage1 = mock(ReactiveSubscription.Message.class);
        ReactiveSubscription.Message<String, String> redisMessage2 = mock(ReactiveSubscription.Message.class);
        when(redisMessage1.getMessage()).thenReturn(json1);
        when(redisMessage2.getMessage()).thenReturn(json2);

        when(container.receive(any(ChannelTopic.class)))
                .thenReturn(Flux.just(redisMessage1, redisMessage2));

        doNothing().when(sseService).sendNotification(any(), any());

        // when
        redisSubscriber.subscribeToNotifications();

        // then
        Thread.sleep(200);
        verify(sseService, timeout(1000).atLeastOnce()).sendNotification(eq(userId1), any());
        verify(sseService, timeout(1000).atLeastOnce()).sendNotification(eq(userId2), any());
    }

    @Test
    @DisplayName("Redis 연결 에러 처리 - 계속 구독 유지")
    void subscribeToNotifications_OnRedisError_ShouldContinueSubscribing() {
        // given
        when(container.receive(any(ChannelTopic.class)))
                .thenReturn(Flux.error(new RuntimeException("Redis connection error")));

        // when
        redisSubscriber.subscribeToNotifications();

        // then - 에러가 발생해도 구독이 계속되어야 함 (onErrorContinue로 처리)
        verify(sseService, timeout(1000).never()).sendNotification(any(), any());
    }

    @Test
    @DisplayName("올바른 채널 토픽 구독 확인")
    void subscribeToNotifications_ShouldSubscribeToCorrectChannel() {
        // given
        when(container.receive(any(ChannelTopic.class)))
                .thenReturn(Flux.empty());

        // when
        redisSubscriber.subscribeToNotifications();

        // then
        verify(container, times(1)).receive(ChannelTopic.of("notification:push"));
    }
}

