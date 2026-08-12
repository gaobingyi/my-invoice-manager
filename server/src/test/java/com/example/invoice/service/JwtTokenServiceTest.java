package com.example.invoice.service;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    private final JwtTokenService service = new JwtTokenService("test-secret-0123456789-abcdefghijklmn", 3600);

    @Test
    void roundtrip() {
        String token = service.generate("admin");
        assertEquals("admin", service.parse(token).getSubject());
    }

    @Test
    void tamperedTokenRejected() {
        String token = service.generate("admin");
        // 篡改负载段，签名不再匹配
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertThrows(JwtException.class, () -> service.parse(tampered));
    }

    @Test
    void expiredTokenRejected() {
        JwtTokenService shortLived = new JwtTokenService("test-secret-0123456789-abcdefghijklmn", -1);
        String token = shortLived.generate("admin");
        assertThrows(JwtException.class, () -> service.parse(token));
    }
}
