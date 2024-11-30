package ru.mastkey.cloudservice.util;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import ru.mastkey.cloudservice.client.model.FileContent;
import ru.mastkey.cloudservice.entity.File;
import ru.mastkey.cloudservice.exception.ErrorType;
import ru.mastkey.cloudservice.exception.ServiceException;
import ru.mastkey.model.ErrorResponse;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResponseFactoryTest {

    @Test
    void classTest() {
        var responseFactory = new ResponseFactory();
        assertThat(responseFactory).isNotNull();
    }

    @Test
    void createErrorResponseForMethodArgumentNotValidException_ShouldReturnFormattedErrorResponse() {
        var fieldError1 = new FieldError("TestObject", "field1", "Field1 is invalid");
        var fieldError2 = new FieldError("TestObject", "field2", "Field2 is required");
        var bindingResult = mock(BindingResult.class);

        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError1, fieldError2));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> response = ResponseFactory.createErrorResponseForMethodArgumentNotValidException(ex);

        assertThat(HttpStatus.BAD_REQUEST).isEqualTo(response.getStatusCode());

        var errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(ErrorType.BAD_REQUEST.getStatus()).isEqualTo(errorResponse.getStatus());
        assertThat(ErrorType.BAD_REQUEST.getCode()).isEqualTo(errorResponse.getCode());

        var expectedMessage = "Поле field1: Field1 is invalid; Поле field2: Field2 is required";
        assertThat(expectedMessage).isEqualTo(errorResponse.getMessage());
    }

    @Test
    void createErrorResponseForServiceException_ShouldReturnErrorResponseWithServiceExceptionDetails() {
        var serviceException = new ServiceException(ErrorType.NOT_FOUND, "User not found");

        ResponseEntity<ErrorResponse> response = ResponseFactory.createErrorResponseForServiceException(serviceException);

        assertThat(HttpStatus.NOT_FOUND).isEqualTo(response.getStatusCode());

        var errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(ErrorType.NOT_FOUND.getStatus()).isEqualTo(errorResponse.getStatus());
        assertThat(ErrorType.NOT_FOUND.getCode()).isEqualTo(errorResponse.getCode());
        assertThat("User not found").isEqualTo(errorResponse.getMessage());
    }

    @Test
    void createErrorResponseForServiceException_ShouldHandleDifferentErrorStatuses() {
        var badRequestException = new ServiceException(ErrorType.BAD_REQUEST, "Invalid input");
        ResponseEntity<ErrorResponse> badRequestResponse = ResponseFactory.createErrorResponseForServiceException(badRequestException);

        assertThat(HttpStatus.BAD_REQUEST).isEqualTo(badRequestResponse.getStatusCode());
        assertThat("Invalid input").isEqualTo(badRequestResponse.getBody().getMessage());

        var internalErrorException = new ServiceException(ErrorType.INTERNAL_SERVER_ERROR, "Server error");
        ResponseEntity<ErrorResponse> internalErrorResponse = ResponseFactory.createErrorResponseForServiceException(internalErrorException);

        assertThat(HttpStatus.INTERNAL_SERVER_ERROR).isEqualTo(internalErrorResponse.getStatusCode());
        assertThat("Server error").isEqualTo(internalErrorResponse.getBody().getMessage());
    }

    @Test
    void createFileResponse_ShouldReturnFileDownloadResponse() {
        var fileExtension = "txt";
        var content = "Sample content".getBytes();
        var fileStream = new ByteArrayInputStream(content);

        var file = mock(File.class);
        when(file.getFileName()).thenReturn("test");
        when(file.getFileExtension()).thenReturn(fileExtension);

        var fileContent = new FileContent(fileStream, file);

        ResponseEntity<Resource> response = ResponseFactory.createFileResponse(fileContent);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=test.txt");
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void buildPagedResponse_ShouldReturnPagedResponseWithHeaders() {
        var data = List.of("Item1", "Item2");
        var pageRequest = PageRequest.of(0, 2);
        var page = new PageImpl<>(data, pageRequest, data.size());

        ResponseEntity<List<String>> response = ResponseFactory.buildPagedResponse(page);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly("Item1", "Item2");
        assertThat(response.getHeaders().getFirst("Total-Pages")).isEqualTo("1");
    }
}