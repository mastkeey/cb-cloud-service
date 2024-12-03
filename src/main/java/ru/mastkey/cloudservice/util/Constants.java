package ru.mastkey.cloudservice.util;

public class Constants {
    public static final String MSG_FILE_NOT_FOUND = "File with id %s not found";
    public static final String MSG_FILE_INVALID_NAME = "Invalid file name";
    public static final String MSG_USER_NOT_FOUND = "User with id %s not found";
    public static final String MSG_USER_ALREADY_EXIST = "User with name %s already exists";
    public static final String MSG_WORKSPACE_NOT_FOUND = "Workspace with id %s not found";
    public static final String MSG_BUCKET_CREATE_ERROR = "Error while creating bucket: %s";
    public static final String MSG_FILE_UPLOAD_ERROR = "Error uploading file to S3: %s";
    public static final String MSG_FILE_DELETE_ERROR = "Error deleting file to S3: %s";
    public static final String MSG_FILE_DOWNLOAD_ERROR = "Error download file from S3: %s";
    public static final String MSG_FOLDER_CREATE_ERROR = "Error creating folder in S3: %s";
    public static final String MSG_FOLDER_DELETE_ERROR = "Error deleting folder in S3: %s";
    public static final String MSG_WORKSPACE_ALREADY_EXIST = "Workspace %s already exists";
    public static final String MSG_WORKSPACE_NOT_LINKED_TO_USER = "Workspace with ID %s is not linked to user with ID %s.";
    public static final String MSG_FILE_NOT_IN_WORKSPACE = "File with ID %s is not part of workspace with ID %s.";
    public static final String MSG_JWT_ERROR = "Error while extracting JWT token";
}
