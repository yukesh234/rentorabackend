package com.bca.rentora.rentora.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Listing {
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "userid")
        private User owner;

        @Enumerated(EnumType.STRING)
        private CategoryType category;

        private String title;
        private String description;
        private BigDecimal pricePerUnit;
        private String priceUnit;
        private Integer quantity;

        @Enumerated(EnumType.STRING)
        private ListingStatus status;

        // set by the admin when a listing is rejected, cleared when it is resubmitted
        @Column(length = 500)
        private String rejectionReason;

        private String city;
        private String district;
        private Double latitude;
        private Double longitude;

        // nullable — only relevant for venue-style listings (grounds, courts, halls)
        // a rentable item like a tent leaves these null; renter picks it up whenever
        private LocalTime openingTime;
        private LocalTime closingTime;

        @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
        @Builder.Default
        private List<ListingImage> images = new ArrayList<>();

        private Instant createdAt;
        private Instant deletedAt; // null = active, non-null = soft-deleted
}