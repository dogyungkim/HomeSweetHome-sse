package com.homesweet.sse.service;

import com.homesweet.sse.dto.PushNotificationDTO;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.Map;

public interface SseService {
    Flux<ServerSentEvent<Object>> subscribe(Long userId);

    void sendNotification(Long userId, PushNotificationDTO notification);

    void sendNotifications(Map<Long, PushNotificationDTO> notificationMap);

    void deleteAll();
}
