// package com.homesweet.sse.auth;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.springframework.http.HttpHeaders;
// import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
// import org.springframework.mock.web.server.MockServerWebExchange;
// import org.springframework.security.core.context.SecurityContext;
// import org.springframework.web.server.ServerWebExchange;
// import org.springframework.web.server.WebFilterChain;
// import reactor.core.publisher.Mono;
// import reactor.test.StepVerifier;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.*;

// @DisplayName("JwtAuthenticationFilter 테스트")
// class JwtAuthenticationFilterTest {

//     private JwtTokenProvider jwtTokenProvider;
//     private JwtAuthenticationFilter filter;
//     private WebFilterChain filterChain;

//     @BeforeEach
//     void setUp() {
//         jwtTokenProvider = mock(JwtTokenProvider.class);
//         filter = new JwtAuthenticationFilter(jwtTokenProvider);
//         filterChain = mock(WebFilterChain.class);
//         when(filterChain.filter(any())).thenReturn(Mono.empty());
//     }

//     @Test
//     @DisplayName("유효한 JWT 토큰으로 인증 성공")
//     void filter_WithValidJwtToken_ShouldSetAuthentication() {
//         // given
//         String token = "valid.jwt.token";
//         Long userId = 123L;
//         when(jwtTokenProvider.validateAndGetUserId(token)).thenReturn(userId);

//         MockServerHttpRequest request = MockServerHttpRequest.get("/api/sse/connect")
//                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
//                 .build();
//         ServerWebExchange exchange = MockServerWebExchange.from(request);

//         // when
//         Mono<Void> result = filter.filter(exchange, filterChain);

//         // then
//         StepVerifier.create(result)
//                 .verifyComplete();

//         verify(filterChain, times(1)).filter(any());
//         verify(jwtTokenProvider, times(1)).validateAndGetUserId(token);
//     }

//     @Test
//     @DisplayName("개발용 백도어 - 숫자 토큰 (1-20011) 인증 성공")
//     void filter_WithNumericToken_ShouldSetAuthentication() {
//         // given
//         String numericToken = "100";
//         MockServerHttpRequest request = MockServerHttpRequest.get("/api/sse/connect")
//                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + numericToken)
//                 .build();
//         ServerWebExchange exchange = MockServerWebExchange.from(request);

//         // when
//         Mono<Void> result = filter.filter(exchange, filterChain);

//         // then
//         StepVerifier.create(result)
//                 .verifyComplete();

//         verify(filterChain, times(1)).filter(any());
//         verify(jwtTokenProvider, never()).validateAndGetUserId(any());
//     }

//     @Test
//     @DisplayName("개발용 백도어 - 범위 밖 숫자 토큰은 JWT로 처리")
//     void filter_WithOutOfRangeNumericToken_ShouldTryJwtValidation() {
//         // given
//         String outOfRangeToken = "20012"; // 범위 밖
//         when(jwtTokenProvider.validateAndGetUserId(outOfRangeToken)).thenReturn(null);

//         MockServerHttpRequest request = MockServerHttpRequest.get("/api/sse/connect")
//                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + outOfRangeToken)
//                 .build();
//         ServerWebExchange exchange = MockServerWebExchange.from(request);

//         // when
//         Mono<Void> result = filter.filter(exchange, filterChain);

//         // then
//         StepVerifier.create(result)
//                 .verifyComplete();

//         verify(filterChain, times(1)).filter(any());
//         verify(jwtTokenProvider, times(1)).validateAndGetUserId(outOfRangeToken);
//     }

//     @Test
//     @DisplayName("유효하지 않은 JWT 토큰 - 인증 없이 통과")
//     void filter_WithInvalidJwtToken_ShouldContinueWithoutAuthentication() {
//         // given
//         String invalidToken = "invalid.jwt.token";
//         when(jwtTokenProvider.validateAndGetUserId(invalidToken)).thenReturn(null);

//         MockServerHttpRequest request = MockServerHttpRequest.get("/api/sse/connect")
//                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken)
//                 .build();
//         ServerWebExchange exchange = MockServerWebExchange.from(request);

//         // when
//         Mono<Void> result = filter.filter(exchange, filterChain);

//         // then
//         StepVerifier.create(result)
//                 .verifyComplete();

//         verify(filterChain, times(1)).filter(any());
//         verify(jwtTokenProvider, times(1)).validateAndGetUserId(invalidToken);
//     }

//     @Test
//     @DisplayName("토큰이 없는 경우 - 인증 없이 통과")
//     void filter_WithoutToken_ShouldContinueWithoutAuthentication() {
//         // given
//         MockServerHttpRequest request = MockServerHttpRequest.get("/api/sse/connect")
//                 .build();
//         ServerWebExchange exchange = MockServerWebExchange.from(request);

//         // when
//         Mono<Void> result = filter.filter(exchange, filterChain);

//         // then
//         StepVerifier.create(result)
//                 .verifyComplete();

//         verify(filterChain, times(1)).filter(any());
//         verify(jwtTokenProvider, never()).validateAndGetUserId(any());
//     }

//     @Test
//     @DisplayName("Actuator 엔드포인트는 인증 없이 통과")
//     void filter_WithActuatorPath_ShouldSkipAuthentication() {
//         // given
//         MockServerHttpRequest request = MockServerHttpRequest.get("/actuator/health")
//                 .build();
//         ServerWebExchange exchange = MockServerWebExchange.from(request);

//         // when
//         Mono<Void> result = filter.filter(exchange, filterChain);

//         // then
//         StepVerifier.create(result)
//                 .verifyComplete();

//         verify(filterChain, times(1)).filter(any());
//         verify(jwtTokenProvider, never()).validateAndGetUserId(any());
//     }

//     @Test
//     @DisplayName("Bearer 접두사가 없는 토큰 - 인증 없이 통과")
//     void filter_WithTokenWithoutBearerPrefix_ShouldContinueWithoutAuthentication() {
//         // given
//         String token = "some.token.without.bearer";
//         MockServerHttpRequest request = MockServerHttpRequest.get("/api/sse/connect")
//                 .header(HttpHeaders.AUTHORIZATION, token)
//                 .build();
//         ServerWebExchange exchange = MockServerWebExchange.from(request);

//         // when
//         Mono<Void> result = filter.filter(exchange, filterChain);

//         // then
//         StepVerifier.create(result)
//                 .verifyComplete();

//         verify(filterChain, times(1)).filter(any());
//         verify(jwtTokenProvider, never()).validateAndGetUserId(any());
//     }

//     @Test
//     @DisplayName("JWT 검증 중 예외 발생 - 인증 없이 통과")
//     void filter_WithJwtValidationException_ShouldContinueWithoutAuthentication() {
//         // given
//         String token = "valid.jwt.token";
//         when(jwtTokenProvider.validateAndGetUserId(token))
//                 .thenThrow(new RuntimeException("JWT validation error"));

//         MockServerHttpRequest request = MockServerHttpRequest.get("/api/sse/connect")
//                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
//                 .build();
//         ServerWebExchange exchange = MockServerWebExchange.from(request);

//         // when
//         Mono<Void> result = filter.filter(exchange, filterChain);

//         // then
//         StepVerifier.create(result)
//                 .verifyComplete();

//         verify(filterChain, times(1)).filter(any());
//         verify(jwtTokenProvider, times(1)).validateAndGetUserId(token);
//     }

//     @Test
//     @DisplayName("개발용 백도어 - 최소값 경계 테스트")
//     void filter_WithMinimumNumericToken_ShouldSetAuthentication() {
//         // given
//         String minToken = "1";
//         MockServerHttpRequest request = MockServerHttpRequest.get("/api/sse/connect")
//                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + minToken)
//                 .build();
//         ServerWebExchange exchange = MockServerWebExchange.from(request);

//         // when
//         Mono<Void> result = filter.filter(exchange, filterChain);

//         // then
//         StepVerifier.create(result)
//                 .verifyComplete();

//         verify(filterChain, times(1)).filter(any());
//         verify(jwtTokenProvider, never()).validateAndGetUserId(any());
//     }

//     @Test
//     @DisplayName("개발용 백도어 - 최대값 경계 테스트")
//     void filter_WithMaximumNumericToken_ShouldSetAuthentication() {
//         // given
//         String maxToken = "20011";
//         MockServerHttpRequest request = MockServerHttpRequest.get("/api/sse/connect")
//                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + maxToken)
//                 .build();
//         ServerWebExchange exchange = MockServerWebExchange.from(request);

//         // when
//         Mono<Void> result = filter.filter(exchange, filterChain);

//         // then
//         StepVerifier.create(result)
//                 .verifyComplete();

//         verify(filterChain, times(1)).filter(any());
//         verify(jwtTokenProvider, never()).validateAndGetUserId(any());
//     }
// }

