package ru.mastkey.cloudservice.exception.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.mastkey.cloudservice.exception.ServiceException;
import ru.mastkey.cloudservice.util.ResponseFactory;
import ru.mastkey.model.ErrorResponse;

@ControllerAdvice
public class CustomExceptionHandler {
    @ExceptionHandler(value = ServiceException.class)
    public ResponseEntity<ErrorResponse> handleServiceException(ServiceException ex) {
        return ResponseFactory.createErrorResponseForServiceException(ex);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        return ResponseFactory.createErrorResponseForMethodArgumentNotValidException(ex);
    }
}
