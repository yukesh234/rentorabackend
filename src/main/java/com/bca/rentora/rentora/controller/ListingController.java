package com.bca.rentora.rentora.controller;

import com.bca.rentora.rentora.dtos.listings.ListingDetailDto;
import com.bca.rentora.rentora.dtos.listings.ListingReqDto;
import com.bca.rentora.rentora.dtos.listings.QuantityUpdateDto;
import com.bca.rentora.rentora.entity.User;
import com.bca.rentora.rentora.helpers.AuthHelper;
import com.bca.rentora.rentora.repo.UserRepo;
import com.bca.rentora.rentora.security.JWTService;
import com.bca.rentora.rentora.services.ListingService;
import com.bca.rentora.rentora.services.implementation.GeminiService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/api/v1/listings")
public class ListingController {
    private final ListingService listingService;
    private final AuthHelper authHelper;
    private final JWTService jwtService;
    private final UserRepo userRepo;
    private final GeminiService  geminiService;


    public ListingController(ListingService listingService, AuthHelper authHelper, JWTService jwtService, UserRepo userRepo, GeminiService geminiService) {
        this.listingService = listingService;
        this.authHelper = authHelper;
        this.jwtService = jwtService;
        this.userRepo = userRepo;
        this.geminiService = geminiService;
    }

    // Resolves the current user's ID from the Authorization header, or null if absent/invalid
    private UUID resolveCurrentUserId(HttpServletRequest request) {
        String token = authHelper.getAccessToken(request);
        if (token == null) return null;

        try {
            String email = jwtService.getEmailFromJWT(token); // adjust to your JwtService's actual method name
            System.out.println(email);
            return userRepo.findByEmail(email)
                    .map(User::getUserid)
                    .orElse(null);
        } catch (Exception e) {
            return null; // invalid/expired token -> treat as anonymous rather than 500
        }
    }

    // Public feed: works for anonymous AND logged-in users.
    // If logged in, excludes the caller's own listings.
    @GetMapping
    public ResponseEntity<List<ListingReqDto>> getFeed(HttpServletRequest request) {
        UUID currentUserId = resolveCurrentUserId(request);
        System.out.println("getFeed resolved currentUserId: " + currentUserId);
        return ResponseEntity.ok(listingService.getFeed(currentUserId));
    }

    // Owner dashboard: requires authentication, returns only the caller's own listings
    @GetMapping("/my")
    public ResponseEntity<List<ListingReqDto>> getMyListings(HttpServletRequest request) {
        UUID currentUserId = resolveCurrentUserId(request);
        if (currentUserId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(listingService.getMyListings(currentUserId));
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ListingReqDto> addProduct(
            @RequestPart("listing") ListingReqDto dto,
            @RequestPart(value = "files", required = false) MultipartFile[] files,
            HttpServletRequest request) throws IOException {
        UUID currentUserId = resolveCurrentUserId(request);
        if (currentUserId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(listingService.addProduct(dto, files, currentUserId));
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ListingReqDto> updateProduct(
            @PathVariable UUID id,
            @RequestPart("listing") ListingReqDto dto,
            @RequestPart(value = "files", required = false) MultipartFile[] files,
            HttpServletRequest request) throws IOException {
        UUID currentUserId = resolveCurrentUserId(request);
        if (currentUserId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(listingService.updateProduct(id, dto, files, currentUserId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id, HttpServletRequest request) {
        UUID currentUserId = resolveCurrentUserId(request);
        if (currentUserId == null) {
            return ResponseEntity.status(401).build();
        }
        listingService.deleteProduct(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/generate-description", consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, String>> generateDescription(
            @RequestPart("files") MultipartFile[] files,
            @RequestPart(value = "title", required = false) String title,
            @RequestPart(value = "category", required = false) String category,
            HttpServletRequest request) throws IOException {

        UUID currentUserId = resolveCurrentUserId(request);
        if (currentUserId == null) {
            return ResponseEntity.status(401).build();
        }

        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "At least one image is required"));
        }

        String description = geminiService.generateFromFiles(files, title, category);
        return ResponseEntity.ok(Map.of("description", description));
    }

    @PostMapping("/update-quantity/{id}")
    public ResponseEntity<ListingReqDto> updatelistingquantity(@PathVariable UUID id,
                                                               @Valid @RequestBody QuantityUpdateDto dto,
                                                               HttpServletRequest request) {
        UUID currentUserId = resolveCurrentUserId(request);
        if (currentUserId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok().body(listingService.updateAvailability(id, dto.quantity(), currentUserId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingDetailDto> getListingById(@PathVariable UUID id) {
        return ResponseEntity.ok(listingService.getListingById(id));
    }
}
