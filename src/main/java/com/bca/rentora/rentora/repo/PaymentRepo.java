package com.bca.rentora.rentora.repo;

import com.bca.rentora.rentora.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepo extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByTransactionId(String transactionId);
    Optional<Payment> findByBooking_Id(UUID bookingId);
}