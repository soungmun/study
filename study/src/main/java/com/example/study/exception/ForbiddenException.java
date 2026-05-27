package com.example.study.exception;

/** 인증은 되었으나 해당 리소스에 대한 권한이 없을 때 (HTTP 403). */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}