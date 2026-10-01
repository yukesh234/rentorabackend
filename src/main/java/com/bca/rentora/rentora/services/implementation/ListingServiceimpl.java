package com.bca.rentora.rentora.services.implementation;

import com.bca.rentora.rentora.dtos.listings.ListingDetailDto;
import com.bca.rentora.rentora.dtos.listings.ListingReqDto;
import com.bca.rentora.rentora.dtos.listings.OwnerSummaryDto;
import com.bca.rentora.rentora.entity.*;
import com.bca.rentora.rentora.exceptions.ResourceNotFoundException;
import com.bca.rentora.rentora.repo.ListingImageRepo;
import com.bca.rentora.rentora.repo.ListingRepo;
import com.bca.rentora.rentora.repo.ReviewRepo;
import com.bca.rentora.rentora.repo.UserRepo;
import com.bca.rentora.rentora.services.ImageStorageService;
import com.bca.rentora.rentora.services.ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListingServiceimpl implements ListingService {

    private final ListingRepo listingRepo;
    private final UserRepo userRepo;
    private final ImageStorageService imageStorageService;
    private final ListingImageRepo listingImageRepo;
    private final ReviewRepo reviewRepo;

    @Override
    public ListingReqDto addProduct(ListingReqDto dto, MultipartFile[] files, UUID ownerId) throws IOException {
        User owner = userRepo.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        Listing listing = Listing.builder()
                .owner(owner)
                .category(dto.getCategory())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .pricePerUnit(dto.getPricePerUnit())
                .priceUnit(dto.getPriceUnit())
                .quantity(dto.getQuantity())
                .status(ListingStatus.PENDING_REVIEW) // was: dto.getStatus()
                .city(dto.getCity())
                .district(dto.getDistrict())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .openingTime(dto.getOpeningTime())
                .closingTime(dto.getClosingTime())
                .createdAt(Instant.now())
                .build();

        Listing saved = listingRepo.save(listing);

        if (files != null) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue; // skip empty parts, avoids Cloudinary throwing on blank uploads

                Map result = imageStorageService.upload(file);
                ListingImage image = ListingImage.builder()
                        .listing(saved)
                        .imageUrl((String) result.get("secure_url"))
                        .publicId((String) result.get("public_id"))
                        .build();
                listingImageRepo.save(image);
            }
        }

        return toDto(saved);
    }

    @Override
    public ListingReqDto updateProduct(UUID id, ListingReqDto dto, MultipartFile[] newFiles, UUID ownerId) throws IOException {
        Listing existing = listingRepo.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        if (!existing.getOwner().getUserid().equals(ownerId)) {
            throw new RuntimeException("You do not own this listing");
        }

        if (dto.getCategory() != null) {
            existing.setCategory(dto.getCategory());
        }

        existing.setTitle(dto.getTitle());
        existing.setDescription(dto.getDescription());
        existing.setPricePerUnit(dto.getPricePerUnit());
        existing.setPriceUnit(dto.getPriceUnit());
        existing.setQuantity(dto.getQuantity());
        existing.setStatus(dto.getStatus());
        existing.setCity(dto.getCity());
        existing.setDistrict(dto.getDistrict());
        existing.setLatitude(dto.getLatitude());
        existing.setLongitude(dto.getLongitude());
        existing.setOpeningTime(dto.getOpeningTime());
        existing.setClosingTime(dto.getClosingTime());

        Listing saved = listingRepo.save(existing);

        // Append any newly uploaded images — existing ones are untouched
        if (newFiles != null) {
            for (MultipartFile file : newFiles) {
                if (file.isEmpty()) continue;

                Map result = imageStorageService.upload(file);
                ListingImage image = ListingImage.builder()
                        .listing(saved)
                        .imageUrl((String) result.get("secure_url"))
                        .publicId((String) result.get("public_id"))
                        .build();
                listingImageRepo.save(image);
            }
        }

        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteProduct(UUID listingId, UUID ownerId) {
        Listing listing = listingRepo.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        if (!listing.getOwner().getUserid().equals(ownerId)) {
            throw new IllegalArgumentException("You do not own this listing");
        }

        if (listing.getDeletedAt() != null) {
            throw new IllegalArgumentException("Listing is already deleted");
        }

        listing.setDeletedAt(Instant.now());
        listing.setStatus(ListingStatus.ARCHIVED); // optional, keeps status consistent
        listingRepo.save(listing);
    }

    @Override
    public List<ListingReqDto> getFeed(UUID excludeOwnerId) {
        return listingRepo.findAllForFeed(excludeOwnerId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<ListingReqDto> getMyListings(UUID ownerId) {
        return listingRepo.findByOwner_useridAndDeletedAtIsNull(ownerId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public ListingReqDto updateAvailability(UUID id, Integer quantity, UUID ownerId) {
        Listing existinglisting = listingRepo.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        if (!existinglisting.getOwner().getUserid().equals(ownerId)) {
            throw new RuntimeException("You do not own this listing");
        }
        existinglisting.setQuantity(quantity);
        listingRepo.save(existinglisting);
        return toDto(existinglisting);
    }

    @Override
    public ListingDetailDto getListingById(UUID id) {
        Listing listing = listingRepo.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        List<Review> reviews = reviewRepo.findByListing_Id(id);
        Double avgRating = reviews.isEmpty()
                ? null
                : reviews.stream().mapToInt(Review::getRating).average().orElse(0);

        return new ListingDetailDto(
                listing.getId(),
                listing.getCategory(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getPricePerUnit(),
                listing.getPriceUnit(),
                listing.getQuantity(),
                listing.getStatus(),
                listing.getCity(),
                listing.getDistrict(),
                listing.getLatitude(),
                listing.getLongitude(),
                listing.getOpeningTime(),
                listing.getClosingTime(),
                listing.getImages().stream().map(ListingImage::getImageUrl).toList(),
                OwnerSummaryDto.builder()
                        .id(listing.getOwner().getUserid())
                        .name(listing.getOwner().getName())
                        .profile_picture(listing.getOwner().getProfilePicture())
                        .build(),
                avgRating,
                (long) reviews.size(),
                listing.getCreatedAt()
        );
    }

    private ListingReqDto toDto(Listing listing) {
        return ListingReqDto.builder()
                .id(listing.getId())
                .category(listing.getCategory())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .pricePerUnit(listing.getPricePerUnit())
                .priceUnit(listing.getPriceUnit())
                .quantity(listing.getQuantity())
                .status(listing.getStatus())
                .city(listing.getCity())
                .district(listing.getDistrict())
                .latitude(listing.getLatitude())
                .longitude(listing.getLongitude())
                .openingTime(listing.getOpeningTime())
                .closingTime(listing.getClosingTime())
                .createdAt(listing.getCreatedAt())
                .imageUrls(listing.getImages().stream()
                        .map(ListingImage::getImageUrl)
                        .toList())
                .owner(OwnerSummaryDto.builder()
                        .id(listing.getOwner().getUserid())
                        .name(listing.getOwner().getName())
                        .profile_picture(listing.getOwner().getProfilePicture())
                        .build())
                .build();
    }
}