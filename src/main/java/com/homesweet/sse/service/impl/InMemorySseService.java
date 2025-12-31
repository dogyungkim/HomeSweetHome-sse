package com.homesweet.sse.service.impl;

import com.homesweet.sse.dto.PushNotificationDTO;
import com.homesweet.sse.service.SseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitResult;
import reactor.core.publisher.Sinks.Many;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class InMemorySseService implements SseService {
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L; // 30분
    private static final int BACKPRESSURE_BUFFER_SIZE = 100; // 최대 버퍼 크기

    private final Map<Long, Many<ServerSentEvent<Object>>> userSinks = new ConcurrentHashMap<>();

    // Initial connection event
    private final ServerSentEvent<Object> connectEvent = ServerSentEvent.builder()
            .event("connect")
            .data("connected")
            .build();

    @Override
    public Flux<ServerSentEvent<Object>> subscribe(Long userId) {
        // PublishSubject 같은거 : 구독한 시점부터 가능 Hot Stream
        Many<ServerSentEvent<Object>> sink = Sinks.many().multicast().onBackpressureBuffer();

        // 사용자 SSE 추가
        userSinks.put(userId, sink);

        log.trace("SSE connection established: userId={}", userId);

        // 연결 이벤트 하나 stream 보내고, 이후에는 sink.asFlux()로 observable 처럼 보낼 수 있음
        return Flux.concat(Flux.just(connectEvent), sink.asFlux())
                .take(Duration.ofMillis(SSE_TIMEOUT_MS))
                .onErrorResume(error -> {
                    log.error("SSE stream error for userId: {}", userId, error);
                    return Flux.empty();
                })
                .doFinally(signal -> {
                    log.trace("SSE connection finalized for userId: {}, signal: {}", userId, signal);
                    removeSink(userId);
                });
    }

    @Override
    public void sendNotification(Long userId, PushNotificationDTO notification) {
        Many<ServerSentEvent<Object>> sink = userSinks.get(userId);
        if (sink == null) {
            log.trace("No active SSE connections for userId: {}", userId);
            return;
        }

        ServerSentEvent<Object> event = ServerSentEvent.builder()
                .event("notification")
                .data(notification)
                .build();

        // tryEmitNext: onNext() 같은거
        EmitResult result = sink.tryEmitNext(event);

        if (result.isFailure()) {
            log.error("SSE 알림 전송 실패: userId={}, result={}", userId, result);
            removeSink(userId);
        }
        log.trace("SSE 알림 전송 성공: userId={}", userId);
    }

    @Override
    public void sendNotifications(Map<Long, PushNotificationDTO> notificationMap) {
        notificationMap.forEach(this::sendNotification);
    }

    @Override
    public void deleteAll() {
        userSinks.values().forEach(sink -> sink.tryEmitComplete());
        userSinks.clear();
        log.info("All SSE connections cleared");
    }

    /**
     * 활성 사용자 수를 반환 (메트릭용)
     */
    public long getActiveUserCount() {
        return userSinks.size();
    }

    /**
     * 사용자의 Sink를 제거하는 헬퍼 메서드
     */
    private void removeSink(Long userId) {
        userSinks.remove(userId);
    }
}
