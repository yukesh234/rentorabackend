package com.bca.rentora.rentora.services;

import com.bca.rentora.rentora.dtos.auth.UserDto;
import com.bca.rentora.rentora.dtos.auth.UserRequestDto;
import com.bca.rentora.rentora.dtos.auth.UserUpdateDto;
import com.bca.rentora.rentora.dtos.user.ChangePasswordDto;
import com.bca.rentora.rentora.dtos.user.UpdateProfileDto;
import com.bca.rentora.rentora.dtos.user.UserProfileDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface Userservice {
    UserProfileDto getProfile(UUID userId);
    UserProfileDto updateProfile(UUID userId, UpdateProfileDto dto);
    UserProfileDto uploadProfilePicture(UUID userId, MultipartFile file);
    void changePassword(UUID userId, ChangePasswordDto dto);
    UserDto createUser(UserRequestDto userRequestDto);
}
