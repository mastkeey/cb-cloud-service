package ru.mastkey.cloudservice.util;

import jakarta.annotation.Nonnull;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import ru.mastkey.cloudservice.client.model.FileContent;
import ru.mastkey.cloudservice.exception.ErrorType;
import ru.mastkey.cloudservice.exception.ServiceException;
import ru.mastkey.model.ErrorResponse;

import java.util.List;
import java.util.stream.Collectors;

public class ResponseFactory {
    public static ResponseEntity<ErrorResponse> createErrorResponseForMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String errorMessages = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    return "Поле " + fieldName + ": " + errorMessage;
                })
                .collect(Collectors.joining("; "));

        var errorResponse = new ErrorResponse()
                .status(ErrorType.BAD_REQUEST.getStatus())
                .code(ErrorType.BAD_REQUEST.getCode())
                .message(errorMessages);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    public static ResponseEntity<ErrorResponse> createErrorResponseForServiceException(ServiceException ex) {
        var errorResponse = new ErrorResponse()
                .status(ex.getStatus())
                .code(ex.getCode())
                .message(ex.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.valueOf(ex.getStatus()));
    }

    public static ResponseEntity<Resource> createFileResponse(FileContent fileContent) {
        var file = fileContent.file();
        var fileStream = fileContent.inputStream();
        var fileName = FileUtils.getFullFileName(file.getFileName(), file.getFileExtension());
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);


        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(fileStream));
    }

    public static <T> ResponseEntity<List<T>> buildPagedResponse(@Nonnull Page<T> page) {
        var headers = new HttpHeaders();
        headers.add("Total-Pages", String.valueOf(page.getTotalPages()));
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }
}
