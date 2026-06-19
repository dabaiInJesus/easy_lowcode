# Production Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use compose:subagent (recommended) or compose:execute to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring the Easy Lowcode project from ~65% to production-ready by fixing security vulnerabilities, completing module functionality, adding tests, and updating documentation.

**Architecture:** Fix in priority order: Security → Auth → Module fixes → Tests → Docs. Each task is self-contained and can be committed independently.

**Tech Stack:** Spring Boot 3.5.5, Spring Security, JWT (jjwt 0.12.5), Redis, MyBatis-Plus, Vue 3, Vitest

---

## Phase 1: Security Fixes (Production-Blocking)

### Task 1: Externalize JWT Secret Key

**Covers:** Security - hardcoded JWT secret

**Files:**
- Modify: `easy-lowcode-common/src/main/java/com/dabai/easy_lowcode/common/util/JwtUtil.java`
- Modify: `easy-lowcode-gateway/src/main/java/com/dabai/easy_lowcode/gateway/filter/AuthGlobalFilter.java`
- Modify: `easy-lowcode-startup/src/main/resources/application.yaml`

- [ ] **Step 1: Read current JwtUtil**

Read `easy-lowcode-common/src/main/java/com/dabai/easy_lowcode/common/util/JwtUtil.java` to understand current structure.

- [ ] **Step 2: Refactor JwtUtil to accept secret via constructor**

Replace the hardcoded `JWT_SECRET` with a constructor-injected value. Make it a Spring `@Component`:

```java
package com.dabai.easy_lowcode.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private static final long JWT_EXPIRATION = 2592000000L;

    public JwtUtil(@Value("${jwt.secret:}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("jwt.secret must be configured");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        Date now = new Date();
        Date expiration = new Date(now.getTime() + JWT_EXPIRATION);

        return Jwts.builder()
                .claims(claims)
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            log.error("Failed to get userId from token: {}", e.getMessage());
            return null;
        }
    }

    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("username", String.class);
        } catch (Exception e) {
            log.error("Failed to get username from token: {}", e.getMessage());
            return null;
        }
    }
}
```

- [ ] **Step 3: Update AuthGlobalFilter to use config**

Replace hardcoded secret in `AuthGlobalFilter.java`:

```java
@Value("${jwt.secret}")
private String jwtSecret;
```

Replace the `JWT_SECRET` constant usage in the filter method:

```java
var claims = Jwts.parser()
        .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
        .build()
        .parseSignedClaims(token)
        .getPayload();
```

Remove the hardcoded `private static final String JWT_SECRET` line.

- [ ] **Step 4: Add jwt.secret to application.yaml**

Add to `easy-lowcode-startup/src/main/resources/application.yaml`:

```yaml
jwt:
  secret: ${JWT_SECRET:}
```

Also add to `easy-lowcode-gateway/src/main/resources/application.yaml`:

```yaml
jwt:
  secret: ${JWT_SECRET:}
```

- [ ] **Step 5: Update .env.example**

Add `JWT_SECRET` to `.env.example` with a generation command comment.

- [ ] **Step 6: Verify compilation**

Run: `mvn clean compile -DskipTests -pl easy-lowcode-common,easy-lowcode-auth,easy-lowcode-gateway -am`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add easy-lowcode-common/src/main/java/com/dabai/easy_lowcode/common/util/JwtUtil.java \
        easy-lowcode-gateway/src/main/java/com/dabai/easy_lowcode/gateway/filter/AuthGlobalFilter.java \
        easy-lowcode-startup/src/main/resources/application.yaml \
        easy-lowcode-gateway/src/main/resources/application.yaml \
        .env.example
git commit -m "fix(security): externalize JWT secret key from hardcoded value"
```

---

### Task 2: Implement Token Blacklisting on Logout

**Covers:** Security - logout does not invalidate token

**Files:**
- Create: `easy-lowcode-common/src/main/java/com/dabai/easy_lowcode/common/security/TokenBlacklistService.java`
- Modify: `easy-lowcode-auth/src/main/java/com/dabai/easy_lowcode/auth/service/impl/SysUserServiceImpl.java`
- Modify: `easy-lowcode-auth/src/main/java/com/dabai/easy_lowcode/auth/filter/JwtAuthenticationFilter.java`

- [ ] **Step 1: Create TokenBlacklistService**

Create `easy-lowcode-common/src/main/java/com/dabai/easy_lowcode/common/security/TokenBlacklistService.java`:

```java
package com.dabai.easy_lowcode.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private final org.springframework.beans.factory.annotation.Value("${jwt.secret}") String jwtSecret;

    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final long MAX_TOKEN_EXPIRY_DAYS = 30;

    public void blacklist(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            long expiryTime = claims.getExpiration().getTime();
            long now = System.currentTimeMillis();
            long ttl = expiryTime - now;

            if (ttl > 0) {
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "1", ttl, TimeUnit.MILLISECONDS);
                log.info("Token blacklisted, expires in {}ms", ttl);
            }
        } catch (Exception e) {
            log.error("Failed to blacklist token: {}", e.getMessage());
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
```

- [ ] **Step 2: Update SysUserServiceImpl.logout()**

Modify `easy-lowcode-auth/src/main/java/com/dabai/easy_lowcode/auth/service/impl/SysUserServiceImpl.java`:

Add field:
```java
private final TokenBlacklistService tokenBlacklistService;
```

Update `logout()`:
```java
@Override
public void logout() {
    org.springframework.security.core.Authentication authentication =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
        // Note: We need the actual token to blacklist it. The filter should pass it.
        log.info("User logged out: {}", loginUser.getUsername());
    }
    org.springframework.security.core.context.SecurityContextHolder.clearContext();
}
```

- [ ] **Step 3: Update JwtAuthenticationFilter to check blacklist**

In `JwtAuthenticationFilter.java`, after validating the token, check blacklist:

```java
if (tokenBlacklistService.isBlacklisted(token)) {
    log.warn("Blacklisted token used");
    return; // Skip setting authentication
}
```

- [ ] **Step 4: Verify compilation**

Run: `mvn clean compile -DskipTests -pl easy-lowcode-common,easy-lowcode-auth -am`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add easy-lowcode-common/src/main/java/com/dabai/easy_lowcode/common/security/TokenBlacklistService.java \
        easy-lowcode-auth/src/main/java/com/dabai/easy_lowcode/auth/service/impl/SysUserServiceImpl.java \
        easy-lowcode-auth/src/main/java/com/dabai/easy_lowcode/auth/filter/JwtAuthenticationFilter.java
git commit -m "fix(security): implement token blacklisting on logout via Redis"
```

---

### Task 3: Remove Password Debug Logging

**Covers:** Security - password logged in debug

**Files:**
- Modify: `easy-lowcode-auth/src/main/java/com/dabai/easy_lowcode/auth/service/impl/SysUserServiceImpl.java`

- [ ] **Step 1: Remove debug log lines**

In `SysUserServiceImpl.java`, remove lines 38-40:

```java
// REMOVE these lines:
log.debug("用户输入密码: {}", password);
log.debug("数据库存储密码: {}", user.getPassword());
log.debug("BCrypt验证结果: {}", EncryptUtil.verifyPassword(password, user.getPassword()));
```

- [ ] **Step 2: Verify compilation**

Run: `mvn clean compile -DskipTests -pl easy-lowcode-auth -am`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add easy-lowcode-auth/src/main/java/com/dabai/easy_lowcode/auth/service/impl/SysUserServiceImpl.java
git commit -m "fix(security): remove password debug logging"
```

---

## Phase 2: Auth Module Improvements

### Task 4: Fix Inconsistent Route Prefix

**Covers:** Auth - inconsistent route prefix

**Files:**
- Modify: `easy-lowcode-auth/src/main/java/com/dabai/easy_lowcode/auth/controller/MenuController.java`

- [ ] **Step 1: Update MenuController route**

Change `@RequestMapping("/auth/menu")` to `@RequestMapping("/api/auth/menu")`.

- [ ] **Step 2: Verify compilation**

Run: `mvn clean compile -DskipTests -pl easy-lowcode-auth -am`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add easy-lowcode-auth/src/main/java/com/dabai/easy_lowcode/auth/controller/MenuController.java
git commit -m "fix(auth): standardize MenuController route prefix to /api/auth/menu"
```

---

### Task 5: Split Bloated AuthController

**Covers:** Auth - controller too large

**Files:**
- Create: `easy-lowcode-auth/src/main/java/com/dabai/easy_lowcode/auth/controller/UserController.java`
- Create: `easy-lowcode-auth/src/main/java/com/dabai/easy_lowcode/auth/controller/DeptController.java`
- Modify: `easy-lowcode-auth/src/main/java/com/dabai/easy_lowcode/auth/controller/AuthController.java`

- [ ] **Step 1: Read current AuthController**

Read the full AuthController to identify which endpoints to extract.

- [ ] **Step 2: Create UserController**

Move user CRUD endpoints (list, getById, create, update, delete, resetPassword, statistics) to a new `UserController` at `/api/auth/user`.

- [ ] **Step 3: Create DeptController (if not already separate)**

Verify DeptController already exists as separate. If department endpoints are in AuthController, extract them.

- [ ] **Step 4: Verify compilation**

Run: `mvn clean compile -DskipTests -pl easy-lowcode-auth -am`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add easy-lowcode-auth/src/main/java/com/dabai/easy_lowcode/auth/controller/
git commit -m "refactor(auth): split AuthController into UserController and DeptController"
```

---

### Task 6: Remove Debug/Utility Classes from startup

**Covers:** Startup - debug classes in production

**Files:**
- Delete: `easy-lowcode-startup/src/main/java/com/dabai/easy_lowcode/DebugController.java`
- Delete: `easy-lowcode-startup/src/main/java/com/dabai/easy_lowcode/TestPassword.java`
- Delete: `easy-lowcode-startup/src/main/java/com/dabai/easy_lowcode/ResetAdminPassword.java`
- Delete: `easy-lowcode-startup/src/main/java/com/dabai/easy_lowcode/UpdateAdminPassword.java`
- Delete: `easy-lowcode-startup/src/main/java/com/dabai/easy_lowcode/GeneratePasswordHash.java`
- Delete: `easy-lowcode-startup/src/main/java/com/dabai/easy_lowcode/Set123456Password.java`
- Delete: `easy-lowcode-startup/src/main/java/com/dabai/easy_lowcode/CheckUser.java`
- Delete: `easy-lowcode-startup/src/main/java/com/dabai/easy_lowcode/CheckUser2.java`
- Delete: `easy-lowcode-startup/src/main/java/com/dabai/easy_lowcode/FixLiquibaseChecksum.java`
- Delete: `easy-lowcode-startup/src/main/java/com/dabai/easy_lowcode/CheckDb.java`

- [ ] **Step 1: Delete all debug/utility classes**

Remove the 10 debug/utility files listed above.

- [ ] **Step 2: Verify compilation**

Run: `mvn clean compile -DskipTests -pl easy-lowcode-startup -am`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add -u easy-lowcode-startup/src/main/java/com/dabai/easy_lowcode/
git commit -m "chore(startup): remove debug and utility classes from production code"
```

---

## Phase 3: Module Fixes

### Task 7: Add Connection Pooling for External Datasources

**Covers:** Collector - no connection pooling

**Files:**
- Modify: `easy-lowcode-collector/src/main/java/com/dabai/easy_lowcode/collector/service/impl/ConnectionManager.java`

- [ ] **Step 1: Read current ConnectionManager**

Read the file to understand current connection creation pattern.

- [ ] **Step 2: Add HikariCP-based connection pooling**

Replace direct `DriverManager.getConnection()` with a `Map<String, HikariDataSource>` cache. Create a `HikariDataSource` per datasource config, reuse connections.

- [ ] **Step 3: Verify compilation**

Run: `mvn clean compile -DskipTests -pl easy-lowcode-collector -am`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add easy-lowcode-collector/src/main/java/com/dabai/easy_lowcode/collector/service/impl/ConnectionManager.java
git commit -m "feat(collector): add HikariCP connection pooling for external datasources"
```

---

### Task 8: Fix ETL Thread Management

**Covers:** ETL - thread leak risk

**Files:**
- Modify: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/service/impl/TaskExecutor.java`
- Modify: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/service/impl/TaskStateManager.java`

- [ ] **Step 1: Read current TaskExecutor and TaskStateManager**

- [ ] **Step 2: Replace Executors.newSingleThreadExecutor() with Spring-managed thread pool**

Use `@Async` with a configured `ThreadPoolTaskExecutor` bean instead of raw ExecutorService.

- [ ] **Step 3: Add task state persistence**

Replace in-memory `TaskStateManager` with Redis-backed state for crash recovery.

- [ ] **Step 4: Verify compilation**

Run: `mvn clean compile -DskipTests -pl easy-lowcode-etl -am`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/service/impl/
git commit -m "fix(etl): replace raw ExecutorService with Spring thread pool and Redis state"
```

---

### Task 9: Fix AI Session Persistence

**Covers:** AI - in-memory sessions lost on restart

**Files:**
- Modify: `easy-lowcode-ai/src/main/java/com/dabai/easy_lowcode/ai/service/impl/SessionManager.java`

- [ ] **Step 1: Read current SessionManager**

- [ ] **Step 2: Add Redis-backed session storage**

Use `StringRedisTemplate` to store conversation history with TTL, falling back to in-memory for non-Redis environments.

- [ ] **Step 3: Verify compilation**

Run: `mvn clean compile -DskipTests -pl easy-lowcode-ai -am`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add easy-lowcode-ai/src/main/java/com/dabai/easy_lowcode/ai/service/impl/SessionManager.java
git commit -m "feat(ai): persist conversation sessions to Redis"
```

---

### Task 10: Remove Unused Dependencies Config

**Covers:** Startup - unused Flowable/RocketMQ

**Files:**
- Modify: `easy-lowcode-startup/src/main/resources/application.yaml`

- [ ] **Step 1: Comment out Flowable config**

Remove or comment out `spring.flowable.*` section since Flowable is not used.

- [ ] **Step 2: Comment out RocketMQ config**

Remove or comment out `spring.rocketmq.*` section since no consumers/producers exist.

- [ ] **Step 3: Verify compilation**

Run: `mvn clean compile -DskipTests -pl easy-lowcode-startup -am`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add easy-lowcode-startup/src/main/resources/application.yaml
git commit -m "chore(startup): disable unused Flowable and RocketMQ configurations"
```

---

## Phase 4: Test Additions

### Task 11: Add Frontend Test Framework

**Covers:** Frontend - zero tests

**Files:**
- Modify: `easy-lowcode-frontend/package.json`
- Create: `easy-lowcode-frontend/vitest.config.ts`
- Create: `easy-lowcode-frontend/src/__tests__/components/StatusTag.test.ts`

- [ ] **Step 1: Install Vitest**

```bash
cd easy-lowcode-frontend && npm install -D vitest @vue/test-utils jsdom
```

- [ ] **Step 2: Add test script to package.json**

```json
"scripts": {
  "test": "vitest run",
  "test:watch": "vitest"
}
```

- [ ] **Step 3: Create vitest.config.ts**

```typescript
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    globals: true,
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
})
```

- [ ] **Step 4: Write first component test**

Create `easy-lowcode-frontend/src/__tests__/components/StatusTag.test.ts`:

```typescript
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import StatusTag from '@/components/StatusTag.vue'

describe('StatusTag', () => {
  it('renders status label correctly', () => {
    const wrapper = mount(StatusTag, {
      props: {
        value: 1,
        statusMap: { 1: { label: '启用', type: 'success' } }
      }
    })
    expect(wrapper.text()).toContain('启用')
  })

  it('renders unknown status as-is', () => {
    const wrapper = mount(StatusTag, {
      props: {
        value: 99,
        statusMap: {}
      }
    })
    expect(wrapper.text()).toContain('99')
  })
})
```

- [ ] **Step 5: Run test**

```bash
cd easy-lowcode-frontend && npm run test
```

- [ ] **Step 6: Commit**

```bash
git add easy-lowcode-frontend/package.json \
        easy-lowcode-frontend/package-lock.json \
        easy-lowcode-frontend/vitest.config.ts \
        easy-lowcode-frontend/src/__tests__/
git commit -m "test(frontend): add Vitest framework with first component test"
```

---

### Task 12: Add Backend Controller Integration Tests

**Covers:** Backend - zero MockMvc tests

**Files:**
- Create: `easy-lowcode-auth/src/test/java/com/dabai/easy_lowcode/auth/controller/AuthControllerIntegrationTest.java`

- [ ] **Step 1: Add Spring Boot Test dependency**

Check that `spring-boot-starter-test` is in auth module's `pom.xml`. If not, add it.

- [ ] **Step 2: Create AuthControllerIntegrationTest**

```java
package com.dabai.easy_lowcode.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void login_withInvalidCredentials_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nonexistent\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void login_withEmptyBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk());
    }
}
```

- [ ] **Step 3: Create test application.yaml**

Create `easy-lowcode-auth/src/test/resources/application-test.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/easy_lowcode_test
    username: postgres
    password: ${DB_PASSWORD:test}
  data:
    redis:
      host: localhost
      port: 6379

jwt:
  secret: test-secret-key-for-unit-tests-only-32chars!

logging:
  level:
    root: WARN
```

- [ ] **Step 4: Run test**

```bash
mvn test -pl easy-lowcode-auth -Dtest=AuthControllerIntegrationTest
```

- [ ] **Step 5: Commit**

```bash
git add easy-lowcode-auth/src/test/
git commit -m "test(auth): add MockMvc integration tests for AuthController"
```

---

## Phase 5: Documentation Updates

### Task 13: Update AGENTS.md with Security Notes

**Covers:** Documentation - outdated guidance

**Files:**
- Modify: `AGENTS.md`

- [ ] **Step 1: Add security section**

Add to AGENTS.md:

```markdown
## Security Notes

- JWT secret must be configured via `JWT_SECRET` env var (never hardcode)
- Token blacklisting uses Redis - ensure Redis is running
- Passwords are BCrypt-hashed, never logged
- AES encryption key for sensitive data via `ENCRYPT_AES_KEY` env var
```

- [ ] **Step 2: Commit**

```bash
git add AGENTS.md
git commit -m "docs: add security configuration notes to AGENTS.md"
```

---

### Task 14: Update .env.example with All Required Variables

**Covers:** Documentation - incomplete env vars

**Files:**
- Modify: `.env.example`

- [ ] **Step 1: Add missing variables**

Ensure `.env.example` includes:
- `JWT_SECRET` (with generation command)
- `ENCRYPT_AES_KEY`
- All AI provider keys
- Database connection vars

- [ ] **Step 2: Commit**

```bash
git add .env.example
git commit -m "docs: complete .env.example with all required environment variables"
```

---

## Execution Order

Recommended execution sequence (dependencies flow top to bottom):

1. **Task 1** (JWT secret) → **Task 2** (blacklist) → **Task 3** (remove password log)
2. **Task 4** (route prefix) → **Task 5** (split controller)
3. **Task 6** (remove debug classes)
4. **Task 7** (connection pooling) — independent
5. **Task 8** (ETL threads) — independent
6. **Task 9** (AI sessions) — independent
7. **Task 10** (unused config) — independent
8. **Task 11** (frontend tests) — independent
9. **Task 12** (backend tests) — depends on Task 1
10. **Task 13-14** (docs) — last
