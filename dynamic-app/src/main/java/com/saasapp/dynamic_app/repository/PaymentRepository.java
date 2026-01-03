package com.saasapp.dynamic_app.repository;

import com.saasapp.dynamic_app.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByRazorpayOrderId(String razorpayOrderId);

    Optional<PaymentEntity> findByRazorpayPaymentId(String razorpayPaymentId);

    List<PaymentEntity> findByUserId(String userId);

    Optional<PaymentEntity> findByUserIdAndRazorpayOrderId(String userId, String razorpayOrderId);

    List<PaymentEntity> findByUserIdOrderByCreatedAtDesc(String userId);
}

