package com.bca.rentora.rentora.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;


@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingImage {
    @Id
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listingid")
    private Listing listing;

    private String imageUrl;
    private String publicId;   // Cloudinary public_id, needed for delete()

    private Integer position;
}
