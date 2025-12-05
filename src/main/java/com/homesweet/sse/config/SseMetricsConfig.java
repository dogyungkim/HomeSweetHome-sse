package com.homesweet.sse.config;

import com.homesweet.sse.service.impl.InMemorySseService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import jakarta.annotation.PostConstruct;

/**
 * SSE 연결 및 메모리 사용량을 모니터링하기 위한 메트릭 설정
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SseMetricsConfig {

    private final MeterRegistry meterRegistry;
    private final InMemorySseService sseService;

    @PostConstruct
    public void registerMetrics() {
        // 활성 SSE 연결 수 모니터링
        Gauge.builder("sse.connections.active", sseService, InMemorySseService::getActiveConnectionCount)
                .description("Number of active SSE connections")
                .register(meterRegistry);

        // 활성 사용자 수 모니터링
        Gauge.builder("sse.users.active", sseService, InMemorySseService::getActiveUserCount)
                .description("Number of users with active SSE connections")
                .register(meterRegistry);

        log.info("SSE metrics registered");
    }

    /**
     * 주기적으로 메트릭 로그 출력 (디버깅용)
     */
    @Scheduled(fixedRate = 60000) // 1분마다
    public void logMetrics() {
        long connectionCount = sseService.getActiveConnectionCount();
        long userCount = sseService.getActiveUserCount();

        if (connectionCount > 0 || userCount > 0) {
            log.info("SSE Metrics - Active Connections: {}, Active Users: {}",
                    connectionCount, userCount);
        }
    }
}
