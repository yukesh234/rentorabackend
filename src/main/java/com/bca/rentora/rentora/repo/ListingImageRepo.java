package com.bca.rentora.rentora.repo;

import com.bca.rentora.rentora.entity.ListingImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ListingImageRepo extends JpaRepository<ListingImage, UUID> {
    List<ListingImage> findByListing_Id(UUID listingId);
}
