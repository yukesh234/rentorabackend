package com.bca.rentora.rentora.services.implementation;

import com.bca.rentora.rentora.dtos.admin.AdminUserSummaryDto;
import com.bca.rentora.rentora.dtos.admin.PendingListingDto;
import com.bca.rentora.rentora.entity.Listing;
import com.bca.rentora.rentora.entity.ListingStatus;
import com.bca.rentora.rentora.entity.User;
import com.bca.rentora.rentora.exceptions.ResourceNotFoundException;
import com.bca.rentora.rentora.repo.ListingRepo;
import com.bca.rentora.rentora.repo.UserRepo;
import com.bca.rentora.rentora.services.AdminModerationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminModerationServiceImpl implements AdminModerationService {

    private final ListingRepo listingRepo;
    private final UserRepo userRepo;

    public AdminModerationServiceImpl(ListingRepo listingRepo, UserRepo userRepo) {
        this.listingRepo = listingRepo;
        this.userRepo = userRepo;
    }

    @Override
    public List<PendingListingDto> getPendingListings() {
        return listingRepo.findByStatusAndDeletedAtIsNull(ListingStatus.PENDING_REVIEW)
                .stream()
                .map(l -> new PendingListingDto(
                        l.getId(), l.getTitle(), l.getCategory().name(),
                        l.getOwner().getUserid(), l.getOwner().getName(), l.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void approveListing(UUID listingId) {
        Listing listing = listingRepo.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        listing.setStatus(ListingStatus.ACTIVE);
        listingRepo.save(listing);
    }

    @Override
    @Transactional
    public void rejectListing(UUID listingId) {
        Listing listing = listingRepo.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        listing.setStatus(ListingStatus.ARCHIVED);
        listingRepo.save(listing);
    }

    @Override
    public List<AdminUserSummaryDto> getAllUsers() {
        List<User> users = userRepo.findAll();
        return users.stream()
                .map(u -> new AdminUserSummaryDto(
                        u.getUserid(), u.getName(), u.getEmail(),
                        listingRepo.countByOwner_UseridAndStatusAndDeletedAtIsNull(u.getUserid(), ListingStatus.ACTIVE),
                        u.getCreatedAt()))
                .collect(Collectors.toList());
    }
}