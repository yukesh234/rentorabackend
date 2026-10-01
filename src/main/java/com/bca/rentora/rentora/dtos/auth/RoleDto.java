package com.bca.rentora.rentora.dtos.auth;



import lombok.*;

import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RoleDto {
    private UUID id;
    private String name;
}
