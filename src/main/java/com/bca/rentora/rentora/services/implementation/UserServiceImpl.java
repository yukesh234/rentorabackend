package com.bca.rentora.rentora.services.implementation;

import com.bca.rentora.rentora.dtos.auth.UserDto;
import com.bca.rentora.rentora.dtos.auth.UserRequestDto;
import com.bca.rentora.rentora.dtos.user.ChangePasswordDto;
import com.bca.rentora.rentora.dtos.user.UpdateProfileDto;
import com.bca.rentora.rentora.dtos.user.UserProfileDto;
import com.bca.rentora.rentora.entity.Provider;
import com.bca.rentora.rentora.entity.Role;
import com.bca.rentora.rentora.entity.User;
import com.bca.rentora.rentora.exceptions.ResourceNotFoundException;
import com.bca.rentora.rentora.repo.UserRepo;
import com.bca.rentora.rentora.services.ImageStorageService;
import com.bca.rentora.rentora.services.Userservice;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class UserServiceImpl implements Userservice {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    // private final CloudinaryService cloudinaryService;
    private final ImageStorageService  imageStorageService;

    public UserServiceImpl(UserRepo userRepo, PasswordEncoder passwordEncoder, ImageStorageService imageStorageService) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.imageStorageService = imageStorageService;
    }


    private UserDto toUserDto(User user) {
        return UserDto.builder()
                .userid(user.getUserid())
                .name(user.getName())
                .email(user.getEmail())
                .enable(user.isEnable())
                .createdAt(user.getCreatedAt())
                .provider(user.getProvider())
                .profilePicture(user.getProfilePicture())
                .profilePicturePublicid(user.getProfilePicturePublicid())
                .build();
    }

    @Override
    public UserProfileDto getProfile(UUID userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toDto(user);
    }

    @Override
    @Transactional
    public UserProfileDto updateProfile(UUID userId, UpdateProfileDto dto) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setName(dto.name().trim());
        userRepo.save(user);

        return toDto(user);
    }

    @Override
    @Transactional
    public UserProfileDto uploadProfilePicture(UUID userId, MultipartFile file) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided");
        }

        try {
            // Clean up the old image first, if one exists
            if (user.getProfilePicturePublicid() != null) {
                imageStorageService.delete(user.getProfilePicturePublicid());
            }

            Map uploadResult = imageStorageService.upload(file);
            String url = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            user.setProfilePicture(url);
            user.setProfilePicturePublicid(publicId);
            userRepo.save(user);
            return toDto(user);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload profile picture", e);
        }
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordDto dto) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getProvider() != null && !"LOCAL".equalsIgnoreCase(String.valueOf(user.getProvider()))) {
            throw new IllegalArgumentException("Password change is not available for accounts signed in via " + user.getProvider());
        }

        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepo.save(user);
    }

    @Override
    @Transactional
    public UserDto createUser(UserRequestDto userRequestDto) {
        if (userRepo.findByEmail(userRequestDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists");
        }
        User user = new User();
        user.setName(userRequestDto.getName());
        user.setEmail(userRequestDto.getEmail());
        user.setPassword(userRequestDto.getPassword()); // already encoded by the caller
        user.setEnable(true);
        user.setProvider(Provider.LOCAL);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepo.save(user);
        return toUserDto(user);
    }

    private UserProfileDto toDto(User user) {
        return new UserProfileDto(
                user.getUserid(),
                user.getName(),
                user.getEmail(),
                user.getProfilePicture(),
                user.getProvider(),
                user.getCreatedAt()
        );
    }
}
