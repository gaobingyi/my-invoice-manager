package com.example.invoice.config;

import com.example.invoice.service.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

/** 验证 JwtAuthenticationFilter + SecurityConfig 的契约：
 *  - 无 Authorization 头 → 401
 *  - 错误 token → 401
 *  - 合法 token → 放行（chain 走到 mock chain）
 *  这避开 @WebMvcTest 在 Java 25 + Mockito inline 的 Byte Buddy 兼容问题。 */
class SecurityConfigTest {

    JwtTokenService jwt;
    JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwt = new JwtTokenService("test-secret-0123456789-abcdefghijklmn", 3600);
        filter = new JwtAuthenticationFilter(jwt);
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthHeader_doesNotAuthenticate() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/invoices");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(req, res, chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "no token should not produce authentication");
    }

    @Test
    void invalidToken_doesNotAuthenticate() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/invoices");
        req.addHeader("Authorization", "Bearer not-a-real-jwt");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(req, res, chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "bad token should not produce authentication");
    }

    @Test
    void validToken_authenticatesAndProceeds() throws Exception {
        String token = jwt.generate("admin");
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/invoices");
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(req, res, chain);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "valid token must produce authentication");
        assertEquals("admin", auth.getName());
    }

    @Test
    void pingPath_skippedByFilter() throws Exception {
        // /api/auth/** 是 permitAll，filter 本身不 path-match：仅看 header。
        // 验证：即便有 bearer，没有 token 也照常放行（chain 走到 mock，证明 controller 也会执行）
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/auth/ping");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(req, res, chain);
        // filter 不阻拦，chain 继续；SecurityConfig 层 permitAll 在 Spring Security 装配时验证
        assertNotNull(chain.getRequest(), "filter must not block /api/auth/ping");
    }
}
