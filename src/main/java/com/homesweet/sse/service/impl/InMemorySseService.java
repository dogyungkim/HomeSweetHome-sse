package com.homesweet.sse.service.impl;

import com.homesweet.sse.dto.PushNotificationDTO;
import com.homesweet.sse.service.SseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitResult;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class InMemorySseService implements SseService {
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L; // 30분
    private static final int BACKPRESSURE_BUFFER_SIZE = 100; // 최대 버퍼 크기

    // 멀티 디바이스 지원: 한 사용자가 여러 디바이스에서 접속 가능
    private final Map<Long, Set<Sinks.Many<ServerSentEvent<Object>>>> userSinks = new ConcurrentHashMap<>();

    // 종료된 Sink를 추적하기 위한 Set
    private final Set<Sinks.Many<ServerSentEvent<Object>>> terminatedSinks = ConcurrentHashMap.newKeySet();

    @Override
    public Flux<ServerSentEvent<Object>> subscribe(Long userId) {
        // Backpressure 전략: 버퍼 크기 100, overflow 시 에러 발생
        Sinks.Many<ServerSentEvent<Object>> sink = Sinks.many()
                .multicast()
                .onBackpressureBuffer(BACKPRESSURE_BUFFER_SIZE, false);

        // 사용자의 Sink Set에 추가 (멀티 디바이스 지원)
        userSinks.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sink);

        log.trace("SSE connection established: userId={}, total connections={}",
                userId, userSinks.get(userId).size());

        // Initial connection event
        ServerSentEvent<Object> connectEvent = ServerSentEvent.builder()
                .event("connect")
                .data("connected")
                .build();

        return Flux.concat(Flux.just(connectEvent), sink.asFlux())
                .take(Duration.ofMillis(SSE_TIMEOUT_MS))
                .onErrorResume(error -> {
                    log.error("SSE stream error for userId: {}", userId, error);
                    return Flux.empty();
                })
                .doOnCancel(() -> {
                    log.trace("SSE connection cancelled for userId: {}", userId);
                    removeSink(userId, sink);
                })
                .doFinally(signal -> {
                    log.trace("SSE connection finalized for userId: {}, signal: {}", userId, signal);
                    removeSink(userId, sink);
                });
    }

    @Override
    public void sendNotification(Long userId, PushNotificationDTO notification) {
        Set<Sinks.Many<ServerSentEvent<Object>>> sinks = userSinks.get(userId);
        if (sinks == null || sinks.isEmpty()) {
            log.trace("No active SSE connections for userId: {}", userId);
            return;
        }

        ServerSentEvent<Object> event = ServerSentEvent.builder()
                .event("notification")
                .data(notification)
                .build();

        // 모든 디바이스에 알림 전송
        sinks.forEach(sink -> {
            EmitResult result = sink.tryEmitNext(event);

            if (result.isFailure()) {
                log.error("SSE 알림 전송 실패: userId={}, result={}", userId, result);
                if (result == EmitResult.FAIL_TERMINATED) {
                    // 종료된 Sink는 즉시 제거 대상으로 표시
                    terminatedSinks.add(sink);
                    log.warn("Marking terminated sink for immediate removal: userId={}", userId);
                }
            } else {
                log.trace("SSE 알림 전송 성공: userId={}", userId);
            }
        });
    }

    @Override
    public void sendNotifications(Map<Long, PushNotificationDTO> notificationMap) {
        notificationMap.forEach(this::sendNotification);
    }

    @Override
    public void deleteAll() {
        userSinks.values().forEach(sinks -> sinks.forEach(sink -> sink.tryEmitComplete()));
        userSinks.clear();
        log.info("All SSE connections cleared");
    }

    /**
     * 활성 연결 수를 반환 (메트릭용)
     */
    public long getActiveConnectionCount() {
        return userSinks.values().stream()
                .mapToLong(Set::size)
                .sum();
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
    private void removeSink(Long userId, Sinks.Many<ServerSentEvent<Object>> sink) {
        Set<Sinks.Many<ServerSentEvent<Object>>> sinks = userSinks.get(userId);
        if (sinks != null) {
            sinks.remove(sink);
            terminatedSinks.remove(sink);
            if (sinks.isEmpty()) {
                userSinks.remove(userId);
            }
        }
    }
}
