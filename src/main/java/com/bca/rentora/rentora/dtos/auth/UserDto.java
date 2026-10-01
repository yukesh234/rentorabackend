package com.bca.rentora.rentora.dtos.auth;

import com.bca.rentora.rentora.entity.Provider;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class UserDto {
    private UUID userid;
    private String email;
    private String name;
    private boolean enable = true;
    private Instant createdAt = Instant.now();
    private Instant UpdatedAt = Instant.now();
    private Provider provider = Provider.LOCAL;
    private Set<RoleDto> roles=new HashSet<>();
    private String profilePicture;
    private String profilePicturePublicid;
}
