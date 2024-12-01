package ru.mastkey.cloudservice.util;

import org.junit.jupiter.api.Test;
import ru.mastkey.cloudservice.entity.Workspace;

import static org.assertj.core.api.Assertions.assertThat;

class FileUtilsTest {

    @Test
    void classTest() {
        FileUtils fileUtils = new FileUtils();
        assertThat(fileUtils).isNotNull();
    }

    @Test
    void getFileNameWithoutExtension_ShouldReturnFileName_WhenExtensionExists() {
        String fileName = "document.txt";
        String result = FileUtils.getFileNameWithoutExtension(fileName);
        assertThat(result).isEqualTo("document");
    }

    @Test
    void getFileNameWithoutExtension_ShouldReturnFileName_WhenNoExtensionExists() {
        String fileName = "document";
        String result = FileUtils.getFileNameWithoutExtension(fileName);
        assertThat(result).isEqualTo("document");
    }

    @Test
    void getFileNameWithoutExtension_ShouldHandleDotInName() {
        String fileName = "my.file.name.txt";
        String result = FileUtils.getFileNameWithoutExtension(fileName);
        assertThat(result).isEqualTo("my.file.name");
    }

    @Test
    void getFileExtension_ShouldReturnExtension_WhenExtensionExists() {
        String fileName = "document.txt";
        String result = FileUtils.getFileExtension(fileName);
        assertThat(result).isEqualTo("txt");
    }

    @Test
    void getFileExtension_ShouldReturnEmptyString_WhenNoExtensionExists() {
        String fileName = "document";
        String result = FileUtils.getFileExtension(fileName);
        assertThat(result).isEqualTo("");
    }

    @Test
    void getFileExtension_ShouldReturnExtension_WhenMultipleDotsExist() {
        String fileName = "archive.tar.gz";
        String result = FileUtils.getFileExtension(fileName);
        assertThat(result).isEqualTo("gz");
    }

    @Test
    void generateRelativePath_ShouldReturnFormattedPath() {
        Workspace workspace = new Workspace();
        workspace.setName("project");
        String fileName = "document";
        String fileExtension = "txt";

        String result = FileUtils.generateRelativePath(workspace.getName(), fileName, fileExtension);
        assertThat(result).isEqualTo("project/document.txt");
    }

    @Test
    void generateRelativePath_ShouldHandleEmptyFileExtension() {
        Workspace workspace = new Workspace();
        workspace.setName("project");
        String fileName = "document";
        String fileExtension = "";

        String result = FileUtils.generateRelativePath(workspace.getName(), fileName, fileExtension);
        assertThat(result).isEqualTo("project/document.");
    }
}