package ru.mastkey.cloudservice.client.model;

import ru.mastkey.cloudservice.entity.File;

import java.io.InputStream;

public record FileContent (
        InputStream inputStream,
        File file
)
{}
