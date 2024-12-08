package ru.mastkey.cloudservice.client;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.mastkey.cloudservice.exception.ErrorType;
import ru.mastkey.cloudservice.exception.ServiceException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;

import static ru.mastkey.cloudservice.util.Constants.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3Client {
    private final MinioClient minioClient;

    public void createBucketIfNotExists(String bucketName) {
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.debug("Bucket '{}' successfully created", bucketName);
            }
        } catch (Exception e) {
            throw new ServiceException(ErrorType.INTERNAL_SERVER_ERROR, MSG_BUCKET_CREATE_ERROR, e.getMessage());
        }
    }

    public void uploadFile(MultipartFile file, String bucketName, String path) {
        try {
            InputStream inputStream = file.getInputStream();
            long fileSize = file.getSize();
            String contentType = file.getContentType();

            if (Objects.isNull(contentType)) {
                contentType = "application/octet-stream";
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .stream(inputStream, fileSize, -1)
                            .contentType(contentType)
                            .build()
            );
            log.debug("File '{}' successfully uploaded to bucket '{}'", path, bucketName);
        } catch (Exception e) {
            log.error("Error uploading file to S3: {}", e.getMessage());
            throw new ServiceException(ErrorType.INTERNAL_SERVER_ERROR, MSG_FILE_UPLOAD_ERROR, e.getMessage());
        }
    }

    public void createFolder(String bucketName, String folderPath) {
        try {
            if (!folderPath.endsWith("/")) {
                folderPath += "/";
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(folderPath)
                            .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                            .build()
            );
            log.debug("Folder '{}' successfully created in bucket '{}'", folderPath, bucketName);
        } catch (Exception e) {
            log.error("Error creating folder in S3: {}", e.getMessage());
            throw new ServiceException(ErrorType.INTERNAL_SERVER_ERROR, MSG_FOLDER_CREATE_ERROR, e.getMessage());
        }
    }

    public void deleteFolder(String bucketName, String folderPath) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .object(folderPath)
                            .bucket(bucketName)
                            .build()
            );
            log.debug("Folder '{}' successfully deleted from bucket '{}'", folderPath, bucketName);
        } catch (Exception e) {
            log.error("Error deleting folder in S3: {}", e.getMessage());
            throw new ServiceException(ErrorType.INTERNAL_SERVER_ERROR, MSG_FOLDER_DELETE_ERROR, e.getMessage());
        }
    }

    public void deleteFile(String bucketName, String filePath) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filePath)
                            .build()
            );
            log.debug("File '{}' successfully deleted from bucket '{}'", filePath, bucketName);
        } catch (Exception e) {
            log.error("Error deleting file in S3: {}", e.getMessage());
            throw new ServiceException(ErrorType.INTERNAL_SERVER_ERROR, MSG_FILE_DELETE_ERROR, e.getMessage());
        }
    }

    public InputStream getFileStream(String bucketName, String filePath) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filePath)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error retrieving file from S3: {}", e.getMessage());
            throw new ServiceException(ErrorType.INTERNAL_SERVER_ERROR, MSG_FILE_DOWNLOAD_ERROR, e.getMessage());
        }
    }
}