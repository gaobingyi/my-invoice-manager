package com.example.invoice.service;

/** 登录失败（用户名不存在或密码错误），由 controller 映射为 401。 */
public class BadCredentialsException extends RuntimeException {
    public BadCredentialsException(String message) {
        super(message);
    }
}
