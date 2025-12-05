package com.homesweet.sse.controller;

import com.homesweet.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@Slf4j
public class SseController {

    private final SseService sseService;

    @GetMapping(value = "/connect", produces = "text/event-stream")
    public Flux<ServerSentEvent<Object>> connect(@AuthenticationPrincipal Long userId) {
        log.info("SSE connection request: userId={}", userId);
        return sseService.subscribe(userId);
    }

    @DeleteMapping("/delete")
    public void delete() {
        sseService.deleteAll();
    }

}
