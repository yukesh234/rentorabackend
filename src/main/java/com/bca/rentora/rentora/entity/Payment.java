package com.bca.rentora.rentora.entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Setter
@Getter
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    private PaymentGateway gateway;

    private String transactionId;      // eSewa transaction_uuid / Khalti pidx
    private String transactionCode;    // eSewa transaction_code returned after verify

    private Double amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private Instant paidAt;

    @Column(updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (status == null) status = PaymentStatus.INITIATED;
    }

    public void setAmount(BigDecimal totalAmount) {
        this.amount = totalAmount.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
}
