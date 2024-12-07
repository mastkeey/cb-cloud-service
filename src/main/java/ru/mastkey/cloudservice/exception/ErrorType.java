package ru.mastkey.cloudservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ErrorType {
    BAD_REQUEST(HttpStatus.BAD_REQUEST.value(), "BadRequest"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value(), "InternalServerError"),
    NOT_FOUND(HttpStatus.NOT_FOUND.value(), "NotFound"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE.value(), "ServiceUnavailable"),
    CONFLICT(HttpStatus.CONFLICT.value(), "Conflict"),
    FORBIDDEN(HttpStatus.FORBIDDEN.value(), "Forbidden"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED.value(), "Unauthorized" );

    private final int status;
    private final String code;
}
