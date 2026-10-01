package com.bca.rentora.rentora.services.implementation;

import com.bca.rentora.rentora.services.ImageStorageService;
import com.cloudinary.Cloudinary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryImageUploadimpl implements ImageStorageService {

    private Cloudinary cloudinary;

    public CloudinaryImageUploadimpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public Map upload(MultipartFile file) throws IOException {
        if(file.isEmpty()){
            throw new IllegalArgumentException("File is empty");
        }
        return this.cloudinary.uploader()
                .upload(file.getBytes(),Map.of());
    }

    @Override
    public Map delete(String publicid) throws IOException {
        if(publicid.isEmpty() ||  publicid.equals("null")){
            throw new IllegalArgumentException("publicid is empty");
        }
        return this.cloudinary
                .uploader().destroy(publicid,Map.of());
    }
}
