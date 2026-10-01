package com.bca.rentora.rentora.services;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public interface ImageStorageService {
    public Map upload(MultipartFile file) throws IOException;
    public Map delete(String publicid) throws IOException;
}
