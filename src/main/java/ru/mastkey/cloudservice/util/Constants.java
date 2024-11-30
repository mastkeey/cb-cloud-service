package ru.mastkey.cloudservice.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
    public final String MSG_FILE_NOT_FOUND = "File with id %s not found";
    public final String MSG_FILE_INVALID_NAME = "Invalid file name";
    public final String MSG_USER_NOT_FOUND = "User with id %s not found";
    public final String MSG_USER_ALREADY_EXIST = "User with id %s already exist";
    public final String MSG_WORKSPACE_NOT_FOUND = "Workspace with id %s not found";
    public final String MSG_USER_DOESNT_HAVE_WORKSPACE = "User with id %s does not have a Workspace with id %s";
    public final String MSG_CURRENT_WORKSPACE_ERROR = "Error with current workspace";
    public final String MSG_BUCKET_CREATE_ERROR = "Error while creating bucket: %s";
    public final String MSG_FILE_UPLOAD_ERROR = "Error uploading file to S3: %s";
    public final String MSG_FILE_DELETE_ERROR = "Error deleting file to S3: %s";
    public final String MSG_FILE_DOWNLOAD_ERROR = "Error download file from S3: %s";
    public final String MSG_FOLDER_CREATE_ERROR = "Error creating folder in S3: %s";
    public final String MSG_FOLDER_DELETE_ERROR = "Error deleting folder in S3: %s";
    public final String MSG_WORKSPACE_ALREADY_EXIST = "Workspace with name %s already exist";
    public final String MSG_LAST_WORKSPACE = "You can't delete last workspace";
    public final String MSG_DELETE_CURRENT_WORKSPACE = "You can't delete current workspace";
}
