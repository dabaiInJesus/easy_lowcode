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

        Field whiteListField = AuthGlobalFilter.class.getDeclaredField("whiteList");
        whiteListField.setAccessible(true);
        whiteListField.set(filter, List.of("/api/auth/login", "/api/auth/register"));

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

        ArgumentCaptor<MockServerWebExchange> captor = ArgumentCaptor.forClass(MockServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        filter.filter(originalExchange, chain).block();

        MockServerWebExchange mutatedExchange = captor.getValue();
        assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("user123");
    }
}
