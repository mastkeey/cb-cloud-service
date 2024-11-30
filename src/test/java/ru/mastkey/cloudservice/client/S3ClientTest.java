package ru.mastkey.cloudservice.client;

import io.minio.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import ru.mastkey.cloudservice.exception.ErrorType;
import ru.mastkey.cloudservice.exception.ServiceException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ClientTest {

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private S3Client s3Client;

    @Mock
    private MultipartFile file;

    private static final String BUCKET_NAME = "test-bucket";
    private static final String FILE_PATH = "test/file/path.txt";
    private static final String FOLDER_PATH = "test/folder/";

    @Test
    void createBucketIfNotExists_ShouldCreateBucket_WhenBucketDoesNotExist() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        s3Client.createBucketIfNotExists(BUCKET_NAME);

        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void createBucketIfNotExists_ShouldNotCreateBucket_WhenBucketExists() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        s3Client.createBucketIfNotExists(BUCKET_NAME);

        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void createBucketIfNotExists_ShouldNotCreateBucket_WhenThrowException() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenThrow(new RuntimeException("Error"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> s3Client.createBucketIfNotExists(BUCKET_NAME));

        assertThat(exception.getCode()).isEqualTo(ErrorType.INTERNAL_SERVER_ERROR.getCode());
    }

    @Test
    void uploadFile_ShouldUploadFileSuccessfully() throws Exception {
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);
        when(file.getInputStream()).thenReturn(inputStream);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("text/plain");

        s3Client.uploadFile(file, BUCKET_NAME, FILE_PATH);

        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void uploadFile_ShouldThrowServiceException_OnFailure() throws Exception {
        when(file.getInputStream()).thenThrow(new RuntimeException("Error"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> s3Client.uploadFile(file, BUCKET_NAME, FILE_PATH));

        assertThat(exception.getCode()).isEqualTo(ErrorType.INTERNAL_SERVER_ERROR.getCode());
    }

    @Test
    void createFolder_ShouldCreateFolderSuccessfully() throws Exception {
        s3Client.createFolder(BUCKET_NAME, FOLDER_PATH);

        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void createFolder_ShouldThrowServiceException_OnFailure() throws Exception {
        doThrow(new RuntimeException("Error")).when(minioClient).putObject(any(PutObjectArgs.class));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> s3Client.createFolder(BUCKET_NAME, FOLDER_PATH));

        assertThat(exception.getCode()).isEqualTo(ErrorType.INTERNAL_SERVER_ERROR.getCode());
    }

    @Test
    void deleteFile_ShouldDeleteFileSuccessfully() throws Exception {
        s3Client.deleteFile(BUCKET_NAME, FILE_PATH);

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void deleteFile_ShouldThrowServiceException_OnFailure() throws Exception {
        doThrow(new RuntimeException("Error")).when(minioClient).removeObject(any(RemoveObjectArgs.class));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> s3Client.deleteFile(BUCKET_NAME, FILE_PATH));

        assertThat(exception.getCode()).isEqualTo(ErrorType.INTERNAL_SERVER_ERROR.getCode());
    }

    @Test
    void getFileStream_ShouldReturnInputStreamSuccessfully() throws Exception {
        GetObjectResponse mockResponse = mock(GetObjectResponse.class);
        byte[] fileData = "test content".getBytes();
        when(mockResponse.readAllBytes()).thenReturn(fileData);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(mockResponse);


        InputStream result = s3Client.getFileStream("test-bucket", "test/path/file.txt");

        assertThat(result).isNotNull();
        assertThat(new String(result.readAllBytes())).isEqualTo("test content");
        verify(minioClient).getObject(any(GetObjectArgs.class));
    }

    @Test
    void getFileStream_ShouldThrowServiceException_OnFailure() throws Exception {
        when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(new RuntimeException("Error"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> s3Client.getFileStream(BUCKET_NAME, FILE_PATH));

        assertThat(exception.getCode()).isEqualTo(ErrorType.INTERNAL_SERVER_ERROR.getCode());
    }

    @Test
    void deleteFolder_ShouldDeleteFolderSuccessfully() throws Exception {
        s3Client.deleteFolder(BUCKET_NAME, FOLDER_PATH);

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void deleteFolder_ShouldThrowServiceException_OnFailure() throws Exception {
        doThrow(new RuntimeException("Error")).when(minioClient).removeObject(any(RemoveObjectArgs.class));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> s3Client.deleteFolder(BUCKET_NAME, FOLDER_PATH));

        assertThat(exception.getCode()).isEqualTo(ErrorType.INTERNAL_SERVER_ERROR.getCode());
        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void uploadFile_ShouldSetDefaultContentType_WhenContentTypeIsNull() throws Exception {
        String bucketName = "test-bucket";
        String path = "test/path/file.txt";
        InputStream inputStream = new ByteArrayInputStream("test data".getBytes());

        when(file.getInputStream()).thenReturn(inputStream);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn(null);

        s3Client.uploadFile(file, bucketName, path);

        verify(file).getInputStream();
        verify(file).getSize();
        verify(file).getContentType();
    }
}