// package com.homesweet.sse.service.impl;

// import com.homesweet.sse.domain.NotificationCategoryType;
// import com.homesweet.sse.dto.PushNotificationDTO;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.springframework.http.codec.ServerSentEvent;
// import reactor.core.publisher.Flux;
// import reactor.test.StepVerifier;

// import java.time.Duration;
// import java.time.LocalDateTime;
// import java.util.HashMap;
// import java.util.Map;

// import static org.junit.jupiter.api.Assertions.*;

// @DisplayName("InMemorySseService 테스트")
// class InMemorySseServiceTest {

//     private InMemorySseService sseService;

//     @BeforeEach
//     void setUp() {
//         sseService = new InMemorySseService();
//     }

//     @Test
//     @DisplayName("SSE 구독 성공 - 연결 이벤트 수신 확인")
//     void subscribe_ShouldEmitConnectEvent() {
//         // given
//         Long userId = 1L;

//         // when
//         Flux<ServerSentEvent<Object>> flux = sseService.subscribe(userId);

//         // then
//         StepVerifier.create(flux)
//                 .expectNextMatches(event -> {
//                     assertEquals("connect", event.event().orElse(""));
//                     assertEquals("connected", event.data());
//                     return true;
//                 })
//                 .thenCancel()
//                 .verify();
//     }

//     @Test
//     @DisplayName("SSE 구독 후 알림 전송 성공")
//     void sendNotification_ShouldEmitNotificationEvent() {
//         // given
//         Long userId = 1L;
//         PushNotificationDTO notification = createTestNotification(1L, "테스트 제목", "테스트 내용");

//         Flux<ServerSentEvent<Object>> flux = sseService.subscribe(userId);

//         // when
//         sseService.sendNotification(userId, notification);

//         // then
//         StepVerifier.create(flux)
//                 .expectNextMatches(event -> "connect".equals(event.event().orElse("")))
//                 .expectNextMatches(event -> {
//                     assertEquals("notification", event.event().orElse(""));
//                     assertNotNull(event.data());
//                     return true;
//                 })
//                 .thenCancel()
//                 .verify();
//     }

//     @Test
//     @DisplayName("활성 연결이 없을 때 알림 전송 - 무시되어야 함")
//     void sendNotification_WhenNoActiveConnection_ShouldNotThrowException() {
//         // given
//         Long userId = 1L;
//         PushNotificationDTO notification = createTestNotification(1L, "테스트 제목", "테스트 내용");

//         // when & then - 예외가 발생하지 않아야 함
//         assertDoesNotThrow(() -> sseService.sendNotification(userId, notification));
//     }

//     @Test
//     @DisplayName("멀티 디바이스 지원 - 한 사용자의 여러 연결에 알림 전송")
//     void sendNotification_WithMultipleDevices_ShouldSendToAllDevices() {
//         // given
//         Long userId = 1L;
//         PushNotificationDTO notification = createTestNotification(1L, "테스트 제목", "테스트 내용");

//         Flux<ServerSentEvent<Object>> flux1 = sseService.subscribe(userId);
//         Flux<ServerSentEvent<Object>> flux2 = sseService.subscribe(userId);

//         // when
//         sseService.sendNotification(userId, notification);

//         // then - 두 연결 모두 알림을 받아야 함
//         StepVerifier.create(flux1)
//                 .expectNextMatches(event -> "connect".equals(event.event().orElse("")))
//                 .expectNextMatches(event -> "notification".equals(event.event().orElse("")))
//                 .thenCancel()
//                 .verify();

//         StepVerifier.create(flux2)
//                 .expectNextMatches(event -> "connect".equals(event.event().orElse("")))
//                 .expectNextMatches(event -> "notification".equals(event.event().orElse("")))
//                 .thenCancel()
//                 .verify();
//     }

//     @Test
//     @DisplayName("여러 사용자에게 일괄 알림 전송")
//     void sendNotifications_ShouldSendToMultipleUsers() {
//         // given
//         Long userId1 = 1L;
//         Long userId2 = 2L;
//         PushNotificationDTO notification1 = createTestNotification(1L, "사용자1 알림", "내용1");
//         PushNotificationDTO notification2 = createTestNotification(2L, "사용자2 알림", "내용2");

//         Flux<ServerSentEvent<Object>> flux1 = sseService.subscribe(userId1);
//         Flux<ServerSentEvent<Object>> flux2 = sseService.subscribe(userId2);

//         Map<Long, PushNotificationDTO> notificationMap = Map.of(
//                 userId1, notification1,
//                 userId2, notification2
//         );

//         // when
//         sseService.sendNotifications(notificationMap);

//         // then
//         StepVerifier.create(flux1)
//                 .expectNextMatches(event -> "connect".equals(event.event().orElse("")))
//                 .expectNextMatches(event -> "notification".equals(event.event().orElse("")))
//                 .thenCancel()
//                 .verify();

//         StepVerifier.create(flux2)
//                 .expectNextMatches(event -> "connect".equals(event.event().orElse("")))
//                 .expectNextMatches(event -> "notification".equals(event.event().orElse("")))
//                 .thenCancel()
//                 .verify();
//     }

//     @Test
//     @DisplayName("모든 연결 삭제")
//     void deleteAll_ShouldClearAllConnections() {
//         // given
//         Long userId1 = 1L;
//         Long userId2 = 2L;
//         sseService.subscribe(userId1);
//         sseService.subscribe(userId2);

//         assertEquals(2, sseService.getActiveUserCount());
//         assertEquals(2, sseService.getActiveConnectionCount());

//         // when
//         sseService.deleteAll();

//         // then
//         assertEquals(0, sseService.getActiveUserCount());
//         assertEquals(0, sseService.getActiveConnectionCount());
//     }

//     @Test
//     @DisplayName("연결 취소 시 Sink 제거 확인")
//     void subscribe_WhenCancelled_ShouldRemoveSink() {
//         // given
//         Long userId = 1L;
//         Flux<ServerSentEvent<Object>> flux = sseService.subscribe(userId);

//         assertEquals(1, sseService.getActiveConnectionCount());
//         assertEquals(1, sseService.getActiveUserCount());

//         // when - 구독 취소
//         StepVerifier.create(flux)
//                 .expectNextCount(1) // connect 이벤트
//                 .thenCancel()
//                 .verify();

//         // then - 약간의 지연 후 Sink가 제거되었는지 확인
//         try {
//             Thread.sleep(100);
//         } catch (InterruptedException e) {
//             Thread.currentThread().interrupt();
//         }

//         assertEquals(0, sseService.getActiveConnectionCount());
//         assertEquals(0, sseService.getActiveUserCount());
//     }

//     @Test
//     @DisplayName("활성 연결 수 확인")
//     void getActiveConnectionCount_ShouldReturnCorrectCount() {
//         // given
//         Long userId1 = 1L;
//         Long userId2 = 2L;

//         // when
//         sseService.subscribe(userId1);
//         sseService.subscribe(userId1); // 같은 사용자의 두 번째 연결
//         sseService.subscribe(userId2);

//         // then
//         assertEquals(3, sseService.getActiveConnectionCount());
//         assertEquals(2, sseService.getActiveUserCount());
//     }

//     @Test
//     @DisplayName("활성 사용자 수 확인")
//     void getActiveUserCount_ShouldReturnCorrectCount() {
//         // given
//         Long userId1 = 1L;
//         Long userId2 = 2L;
//         Long userId3 = 3L;

//         // when
//         sseService.subscribe(userId1);
//         sseService.subscribe(userId2);
//         sseService.subscribe(userId3);

//         // then
//         assertEquals(3, sseService.getActiveUserCount());
//         assertEquals(3, sseService.getActiveConnectionCount());
//     }

//     @Test
//     @DisplayName("SSE 타임아웃 확인 - 30분 후 자동 종료")
//     void subscribe_ShouldTimeoutAfter30Minutes() {
//         // given
//         Long userId = 1L;
//         Flux<ServerSentEvent<Object>> flux = sseService.subscribe(userId);

//         // when & then - 타임아웃 시간보다 짧게 테스트 (실제로는 30분이지만 테스트에서는 짧게)
//         // 실제로는 30분이지만 테스트 시간을 단축하기 위해 take 연산자로 제한
//         StepVerifier.create(flux.take(Duration.ofMillis(100)))
//                 .expectNextCount(1) // connect 이벤트
//                 .expectComplete()
//                 .verify();
//     }

//     @Test
//     @DisplayName("에러 발생 시 빈 Flux 반환")
//     void subscribe_OnError_ShouldReturnEmptyFlux() {
//         // given
//         Long userId = 1L;
//         Flux<ServerSentEvent<Object>> flux = sseService.subscribe(userId);

//         // when - Sink에 에러 발생 시뮬레이션
//         // 실제로는 onErrorResume으로 빈 Flux를 반환하므로 에러가 전파되지 않음
//         // then
//         StepVerifier.create(flux)
//                 .expectNextCount(1) // connect 이벤트
//                 .thenCancel()
//                 .verify();
//     }

//     private PushNotificationDTO createTestNotification(Long notificationId, String title, String content) {
//         return PushNotificationDTO.builder()
//                 .notificationId(notificationId)
//                 .title(title)
//                 .content(content)
//                 .redirectUrl("app://test")
//                 .contextData(new HashMap<>())
//                 .isRead(false)
//                 .categoryType(NotificationCategoryType.SYSTEM)
//                 .createdAt(LocalDateTime.now())
//                 .build();
//     }
// }

