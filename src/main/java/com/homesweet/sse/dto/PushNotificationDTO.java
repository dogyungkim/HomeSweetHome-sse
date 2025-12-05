package com.homesweet.sse.dto;

import java.util.Map;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.homesweet.sse.domain.NotificationCategoryType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PushNotificationDTO {
    Long notificationId;
    String title;
    String content;
    String redirectUrl;
    Map<String, Object> contextData;

    @JsonProperty("read")
    boolean isRead;

    NotificationCategoryType categoryType;
    LocalDateTime createdAt;
}
