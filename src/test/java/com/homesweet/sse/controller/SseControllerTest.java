package com.homesweet.sse.controller;

import com.homesweet.sse.service.SseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import static org.mockito.Mockito.*;

@DisplayName("SseController 테스트")
class SseControllerTest {

    private SseService sseService;
    private SseController sseController;
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        sseService = mock(SseService.class);
        sseController = new SseController(sseService);
        webTestClient = WebTestClient.bindToController(sseController).build();
    }

    @Test
    @DisplayName("SSE 연결 엔드포인트 테스트")
    @WithMockUser(username = "1")
    void connect_ShouldReturnSSEStream() {
        // given
        Long userId = 1L;
        ServerSentEvent<Object> connectEvent = ServerSentEvent.builder()
                .event("connect")
                .data("connected")
                .build();

        when(sseService.subscribe(userId)).thenReturn(Flux.just(connectEvent));

        // when & then
        webTestClient.get()
                .uri("/api/sse/connect")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("text/event-stream;charset=UTF-8");

        verify(sseService, times(1)).subscribe(userId);
    }

    @Test
    @DisplayName("SSE 연결 - 사용자 ID 확인")
    @WithMockUser(username = "123")
    void connect_ShouldUseCorrectUserId() {
        // given
        Long userId = 123L;
        ServerSentEvent<Object> connectEvent = ServerSentEvent.builder()
                .event("connect")
                .data("connected")
                .build();

        when(sseService.subscribe(userId)).thenReturn(Flux.just(connectEvent));

        // when
        webTestClient.get()
                .uri("/api/sse/connect")
                .exchange()
                .expectStatus().isOk();

        // then
        verify(sseService, times(1)).subscribe(userId);
    }

    @Test
    @DisplayName("모든 연결 삭제 엔드포인트 테스트")
    void deleteAll_ShouldCallServiceMethod() {
        // given
        doNothing().when(sseService).deleteAll();

        // when & then
        webTestClient.delete()
                .uri("/api/sse/delete")
                .exchange()
                .expectStatus().isOk();

        verify(sseService, times(1)).deleteAll();
    }

    @Test
    @DisplayName("SSE 연결 - 빈 스트림 반환")
    @WithMockUser(username = "1")
    void connect_WithEmptyStream_ShouldReturnOk() {
        // given
        Long userId = 1L;
        when(sseService.subscribe(userId)).thenReturn(Flux.empty());

        // when & then
        webTestClient.get()
                .uri("/api/sse/connect")
                .exchange()
                .expectStatus().isOk();

        verify(sseService, times(1)).subscribe(userId);
    }

    @Test
    @DisplayName("SSE 연결 - 여러 이벤트 스트림")
    @WithMockUser(username = "1")
    void connect_WithMultipleEvents_ShouldReturnAllEvents() {
        // given
        Long userId = 1L;
        ServerSentEvent<Object> event1 = ServerSentEvent.builder()
                .event("connect")
                .data("connected")
                .build();
        ServerSentEvent<Object> event2 = ServerSentEvent.builder()
                .event("notification")
                .data("test")
                .build();

        when(sseService.subscribe(userId)).thenReturn(Flux.just(event1, event2));

        // when & then
        webTestClient.get()
                .uri("/api/sse/connect")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("text/event-stream;charset=UTF-8");

        verify(sseService, times(1)).subscribe(userId);
    }
}

