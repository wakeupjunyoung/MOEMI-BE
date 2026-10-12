package com.example.moemi.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(new ErrorResponse(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException e) {
        FieldError error = e.getBindingResult().getFieldError();
        String message = error == null ? "요청 값이 올바르지 않습니다." : error.getField() + ": " + error.getDefaultMessage();
        return badRequest("INVALID_REQUEST", message);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleParameterValidation(HandlerMethodValidationException e) {
        String message = e.getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다.");
        return badRequest("INVALID_REQUEST", message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        return badRequest("INVALID_REQUEST", e.getParameterName() + " 파라미터가 필요합니다.");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException e) {
        return badRequest("INVALID_REQUEST", e.getHeaderName() + " 헤더가 필요합니다.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return badRequest("INVALID_REQUEST", e.getName() + " 값의 형식이 올바르지 않습니다.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
        return badRequest("INVALID_REQUEST", "요청 본문을 읽을 수 없습니다. JSON 형식과 UTF-8 인코딩을 확인해 주세요.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", "존재하지 않는 경로입니다: /" + e.getResourcePath()));
    }

    private static ResponseEntity<ErrorResponse> badRequest(String code, String message) {
        return ResponseEntity.badRequest().body(new ErrorResponse(code, message));
    }
}
