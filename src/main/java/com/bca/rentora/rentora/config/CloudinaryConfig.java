package com.bca.rentora.rentora.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Value("${spring.cloudinary.cloudname}")
    private String cloudname;

    @Value("${spring.cloudinary.api-key}")
    private String api_key;

    @Value("${spring.cloudinary.api-secret}")
    private String api_secret;

    @Bean
    public Cloudinary getCloudinary(){
        Map config = new HashMap();
        config.put("cloud_name", cloudname);
        config.put("api_key", api_key);
        config.put("api_secret", api_secret);
        return new Cloudinary(config);
    }
}
