package com.bca.rentora.rentora.dtos.listings;


import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerSummaryDto {
    private UUID id;
    private String name;
    private String profile_picture;
}
