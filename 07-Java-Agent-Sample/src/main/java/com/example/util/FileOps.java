package com.example.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class FileOps {

    public static void saveMetadataToFile(Object metadata, String filePath, ObjectMapper mapper) {
        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs();
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, metadata);
            System.out.println(filePath + " file created successfully");
        } catch (IOException e) {
            System.err.println("Exception in saveMetadataToFile: " + e.getMessage());
        }
    }
}
