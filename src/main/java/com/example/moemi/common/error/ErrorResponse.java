package com.example.moemi.common.error;

/**
 * Error body shared by every endpoint: {@code {"code": "...", "message": "..."}}.
 */
public record ErrorResponse(String code, String message) {
}
