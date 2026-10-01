package com.bca.rentora.rentora.services;

import com.bca.rentora.rentora.dtos.admin.AdminUserSummaryDto;
import com.bca.rentora.rentora.dtos.admin.PendingListingDto;

import java.util.List;
import java.util.UUID;

public interface AdminModerationService {
    List<PendingListingDto> getPendingListings();
    void approveListing(UUID listingId);
    void rejectListing(UUID listingId);
    List<AdminUserSummaryDto> getAllUsers();
}