package com.example.invoice.controller;

import com.example.invoice.dto.LoginRequest;
import com.example.invoice.dto.LoginResponse;
import com.example.invoice.service.AuthService;
import com.example.invoice.service.BadCredentialsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        return authService.login(req, clientIp(request));
    }

    /** 公开探活端点，供 Docker healthcheck（业务端点已 401 保护）。 */
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> badCredentials(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }

    private static String clientIp(HttpServletRequest req) {
        // 优先信任 nginx 设置的 X-Real-IP（不可被客户端伪造）。
        // 不用 X-Forwarded-For：它会追加客户端自带的 XFF 头，split(",")[0] 取到的是
        // 可伪造值，攻击者可借此绕过登录限流并定向陷害某 IP。
        String realIp = req.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return req.getRemoteAddr();
    }
}
