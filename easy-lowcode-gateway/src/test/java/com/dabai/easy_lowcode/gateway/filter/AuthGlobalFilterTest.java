package com.dabai.easy_lowcode.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthGlobalFilterTest {

    private static final String JWT_SECRET = "mySecretKeyForJwtTokenSigning1234567890";
    private AuthGlobalFilter filter;
    private SecretKey hmacKey;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() throws Exception {
        filter = new AuthGlobalFilter();
        hmacKey = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));

        Field secretField = AuthGlobalFilter.class.getDeclaredField("jwtSecret");
        secretField.setAccessible(true);
        secretField.set(filter, JWT_SECRET);

        // 实现已演化为「精确匹配 + 前缀匹配」两个集合（原单一 whiteList 字段已拆分）
        Field exactField = AuthGlobalFilter.class.getDeclaredField("exactWhiteList");
        exactField.setAccessible(true);
        exactField.set(filter, new java.util.HashSet<>(List.of("/api/auth/login", "/api/auth/register")));

        Field prefixField = AuthGlobalFilter.class.getDeclaredField("prefixWhiteList");
        prefixField.setAccessible(true);
        prefixField.set(filter, new java.util.HashSet<>(List.of("/swagger-ui/", "/v3/api-docs/", "/actuator/")));

        filter.init();

        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    private String createToken(String subject) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(hmacKey)
                .compact();
    }

    private MockServerWebExchange createExchange(String path, String authHeader) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get(path);
        if (authHeader != null) {
            builder.header(HttpHeaders.AUTHORIZATION, authHeader);
        }
        MockServerHttpRequest request = (MockServerHttpRequest) builder.build();
        return MockServerWebExchange.from(request);
    }

    @Test
    void getOrder_returnsMinus100() {
        assertThat(filter.getOrder()).isEqualTo(-100);
    }

    @Test
    void whitelistedPath_passesThrough() {
        MockServerWebExchange exchange = createExchange("/api/auth/login", null);
        filter.filter(exchange, chain).block();
        verify(chain).filter(exchange);
    }

    @Test
    void whitelistedPath_register_passesThrough() {
        MockServerWebExchange exchange = createExchange("/api/auth/register", null);
        filter.filter(exchange, chain).block();
        verify(chain).filter(exchange);
    }

    @Test
    void missingAuthorizationHeader_returns401() {
        MockServerWebExchange exchange = createExchange("/api/users", null);
        filter.filter(exchange, chain).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void authorizationHeaderWithoutBearer_returns401() {
        MockServerWebExchange exchange = createExchange("/api/users", "Basic abc123");
        filter.filter(exchange, chain).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void invalidToken_returns401() {
        MockServerWebExchange exchange = createExchange("/api/users", "Bearer invalid.jwt.token");
        filter.filter(exchange, chain).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void validToken_addsXUserIdHeader() {
        String token = createToken("user123");
        MockServerWebExchange originalExchange = createExchange("/api/users", "Bearer " + token);

        filter.filter(originalExchange, chain).block();

        // 官方推荐姿势：capture() 只用于 verify()（stubbing 中使用是 Mockito 明确警告的模式）
        ArgumentCaptor<org.springframework.web.server.ServerWebExchange> captor =
                ArgumentCaptor.forClass(org.springframework.web.server.ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        org.springframework.web.server.ServerWebExchange mutatedExchange = captor.getValue();
        assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("user123");
    }
}
