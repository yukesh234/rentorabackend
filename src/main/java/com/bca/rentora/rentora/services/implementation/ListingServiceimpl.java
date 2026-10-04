package com.bca.rentora.rentora.services.implementation;

import com.bca.rentora.rentora.dtos.listings.ListingDetailDto;
import com.bca.rentora.rentora.dtos.listings.ListingImageDto;
import com.bca.rentora.rentora.dtos.listings.ListingReqDto;
import com.bca.rentora.rentora.dtos.listings.OwnerSummaryDto;
import com.bca.rentora.rentora.entity.*;
import com.bca.rentora.rentora.exceptions.ResourceNotFoundException;
import com.bca.rentora.rentora.repo.BookingRepo;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
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
    private final BookingRepo bookingRepo;

    @Override
    public ListingReqDto addProduct(ListingReqDto dto, MultipartFile[] files, UUID ownerId) throws IOException {
        User owner = userRepo.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        // venues (SPORTS / ENTERTAINMENT) are a single bookable resource
        boolean isVenue = dto.getCategory() != null && dto.getCategory() != CategoryType.UTILITY;
        Integer quantity = isVenue ? Integer.valueOf(1) : dto.getQuantity();

        Listing listing = Listing.builder()
                .owner(owner)
                .category(dto.getCategory())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .pricePerUnit(dto.getPricePerUnit())
                .priceUnit(dto.getPriceUnit())
                .quantity(quantity)
                .status(ListingStatus.PENDING_REVIEW)
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

        // venues are always quantity 1
        boolean isVenue = existing.getCategory() != null && existing.getCategory() != CategoryType.UTILITY;
        existing.setQuantity(isVenue ? Integer.valueOf(1) : dto.getQuantity());

        // Owners can't approve their own listing:
        // - editing a REJECTED listing sends it back to review (this is the "push it again" path)
        // - a PENDING_REVIEW listing keeps its status until an admin decides
        // - otherwise the owner can switch between ACTIVE / INACTIVE / DRAFT / ARCHIVED, but
        //   going live from DRAFT or ARCHIVED needs a review first
        ListingStatus current = existing.getStatus();
        ListingStatus requested = dto.getStatus();

        if (current == ListingStatus.REJECTED) {
            existing.setStatus(ListingStatus.PENDING_REVIEW);
            existing.setRejectionReason(null);
        } else if (current != ListingStatus.PENDING_REVIEW
                && requested != null
                && requested != ListingStatus.PENDING_REVIEW
                && requested != ListingStatus.REJECTED) {
            boolean needsReview = requested == ListingStatus.ACTIVE
                    && (current == ListingStatus.DRAFT || current == ListingStatus.ARCHIVED);
            existing.setStatus(needsReview ? ListingStatus.PENDING_REVIEW : requested);
        }

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
    public ListingReqDto resubmitListing(UUID id, UUID ownerId) {
        Listing listing = listingRepo.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        if (!listing.getOwner().getUserid().equals(ownerId)) {
            throw new IllegalArgumentException("You do not own this listing");
        }

        if (listing.getStatus() != ListingStatus.REJECTED) {
            throw new IllegalArgumentException("Only rejected listings can be resubmitted");
        }

        listing.setStatus(ListingStatus.PENDING_REVIEW);
        listing.setRejectionReason(null);
        listingRepo.save(listing);

        return toDto(listing);
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

        boolean hasActiveBookings = bookingRepo.findByListing_Id(listingId).stream()
                .anyMatch(b -> b.getStatus() != BookingStatus.CANCELLED);
        if (hasActiveBookings) {
            throw new IllegalArgumentException("This listing has existing bookings and cannot be deleted");
        }

        listing.setDeletedAt(Instant.now());
        listing.setStatus(ListingStatus.ARCHIVED);
        listingRepo.save(listing);
    }

    @Override
    public List<ListingReqDto> getFeed(UUID excludeOwnerId, String q, String category,
                                       BigDecimal minPrice, BigDecimal maxPrice, String sort) {
        final String needle = q == null ? "" : q.trim().toLowerCase();
        final CategoryType categoryFilter = parseCategory(category);

        Comparator<Listing> order;
        if ("price_asc".equals(sort)) {
            order = Comparator.comparing(Listing::getPricePerUnit,
                    Comparator.nullsLast(Comparator.<BigDecimal>naturalOrder()));
        } else if ("price_desc".equals(sort)) {
            order = Comparator.comparing(Listing::getPricePerUnit,
                    Comparator.nullsLast(Comparator.<BigDecimal>reverseOrder()));
        } else {
            order = Comparator.comparing(Listing::getCreatedAt,
                    Comparator.nullsLast(Comparator.<Instant>reverseOrder()));
        }

        return listingRepo.findAllForFeed(excludeOwnerId)
                .stream()
                .filter(l -> categoryFilter == null || l.getCategory() == categoryFilter)
                .filter(l -> needle.isEmpty()
                        || contains(l.getTitle(), needle)
                        || contains(l.getDescription(), needle)
                        || contains(l.getCity(), needle)
                        || contains(l.getDistrict(), needle))
                .filter(l -> minPrice == null
                        || (l.getPricePerUnit() != null && l.getPricePerUnit().compareTo(minPrice) >= 0))
                .filter(l -> maxPrice == null
                        || (l.getPricePerUnit() != null && l.getPricePerUnit().compareTo(maxPrice) <= 0))
                .sorted(order)
                .map(this::toDto)
                .toList();
    }

    private boolean contains(String text, String needle) {
        return text != null && text.toLowerCase().contains(needle);
    }

    private CategoryType parseCategory(String category) {
        if (category == null || category.isBlank()) return null;
        try {
            return CategoryType.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null; // unknown category: ignore the filter instead of failing
        }
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
    @Transactional
    public ListingReqDto deleteImage(UUID listingId, UUID imageId, UUID ownerId) throws IOException {
        Listing listing = listingRepo.findActiveById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        if (!listing.getOwner().getUserid().equals(ownerId)) {
            throw new IllegalArgumentException("You do not own this listing");
        }

        ListingImage image = listing.getImages().stream()
                .filter(i -> i.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Image not found on this listing"));

        // remove it from Cloudinary too; if that fails, still remove it from our DB
        if (image.getPublicId() != null && !image.getPublicId().isBlank()) {
            try {
                imageStorageService.delete(image.getPublicId());
            } catch (Exception e) {
                System.err.println("Cloudinary delete failed for " + image.getPublicId() + ": " + e.getMessage());
            }
        }

        // orphanRemoval = true on Listing.images deletes the row
        listing.getImages().remove(image);
        listingRepo.save(listing);

        return toDto(listing);
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
                .rejectionReason(listing.getRejectionReason())
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
                .images(listing.getImages().stream()
                        .map(i -> new ListingImageDto(i.getId(), i.getImageUrl()))
                        .toList())
                .owner(OwnerSummaryDto.builder()
                        .id(listing.getOwner().getUserid())
                        .name(listing.getOwner().getName())
                        .profile_picture(listing.getOwner().getProfilePicture())
                        .build())
                .build();
    }
}