package com.bca.rentora.rentora.controller;

import com.bca.rentora.rentora.dtos.admin.AdminUserSummaryDto;
import com.bca.rentora.rentora.dtos.admin.PendingListingDto;
import com.bca.rentora.rentora.dtos.admin.RejectListingDto;
import com.bca.rentora.rentora.services.AdminModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/super-admin")
@RequiredArgsConstructor
public class AdminModerationController {

    private final AdminModerationService adminModerationService;

    @GetMapping("/listings/pending")
    public ResponseEntity<List<PendingListingDto>> getPendingListings() {
        return ResponseEntity.ok(adminModerationService.getPendingListings());
    }

    @PostMapping("/listings/{id}/approve")
    public ResponseEntity<Void> approveListing(@PathVariable UUID id) {
        adminModerationService.approveListing(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/listings/{id}/reject")
    public ResponseEntity<Void> rejectListing(@PathVariable UUID id,
                                              @RequestBody(required = false) RejectListingDto dto) {
        adminModerationService.rejectListing(id, dto == null ? null : dto.reason());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserSummaryDto>> getUsers() {
        return ResponseEntity.ok(adminModerationService.getAllUsers());
    }
}