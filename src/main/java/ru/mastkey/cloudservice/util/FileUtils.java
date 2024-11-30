package ru.mastkey.cloudservice.util;

import ru.mastkey.cloudservice.entity.Workspace;

public class FileUtils {
    public static String getFileNameWithoutExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex == -1 ? fileName : fileName.substring(0, dotIndex);
    }

    public static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex == -1 ? "" : fileName.substring(dotIndex + 1);
    }

    public static String generateRelativePath(Workspace workspace, String fileName, String fileExtension) {
        return String.format("%s/%s.%s", workspace.getName(), fileName, fileExtension);
    }

    public static String getFullFileName(String fileName, String fileExtension) {
        return String.format("%s.%s", fileName, fileExtension);
    }
}
