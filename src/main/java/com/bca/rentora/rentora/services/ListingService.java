package com.bca.rentora.rentora.services;

import com.bca.rentora.rentora.dtos.listings.ListingDetailDto;
import com.bca.rentora.rentora.dtos.listings.ListingReqDto;
import com.bca.rentora.rentora.entity.Listing;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface ListingService {
        ListingReqDto addProduct(ListingReqDto dto, MultipartFile[] files, UUID ownerId) throws IOException;
        ListingReqDto updateProduct(UUID id, ListingReqDto dto, MultipartFile[] newFiles, UUID ownerId) throws IOException;
        void deleteProduct(UUID id, UUID ownerId);
        List<ListingReqDto> getFeed(UUID excludeOwnerId); // excludeOwnerId nullable
        List<ListingReqDto> getMyListings(UUID ownerId);
        ListingReqDto updateAvailability(UUID id, Integer quantity, UUID ownerId);
        ListingDetailDto getListingById(UUID id);
}
