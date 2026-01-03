package com.saasapp.dynamic_app.service;

import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.saasapp.dynamic_app.dto.CreateOrderResponse;
import com.saasapp.dynamic_app.dto.CreatePaymentRequest;
import com.saasapp.dynamic_app.dto.PaymentResponse;
import com.saasapp.dynamic_app.dto.PaymentVerificationRequest;
import com.saasapp.dynamic_app.entity.PaymentEntity;
import com.saasapp.dynamic_app.repository.PaymentRepository;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    private RazorpayClient razorpayClient;

    @Autowired
    public void initializeRazorpayClient() {
        try {
            razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            logger.info("Razorpay client initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Razorpay client: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize Razorpay client", e);
        }
    }

    /**
     * Create a payment order with Razorpay
     */
    public CreateOrderResponse createPaymentOrder(CreatePaymentRequest request) {
        try {
            logger.debug("Creating payment order for userId: {}, amount: {}", request.getUserId(), request.getAmount());

            // Auto-populate customerEmail from userId if not provided
            String customerEmail = request.getCustomerEmail();
            if (customerEmail == null || customerEmail.isEmpty()) {
                // If userId looks like an email, use it as customerEmail
                if (request.getUserId() != null && request.getUserId().contains("@")) {
                    customerEmail = request.getUserId();
                    logger.info("Auto-populated customerEmail from userId: {}", customerEmail);
                }
            }

            JSONObject orderJson = new JSONObject();
            orderJson.put("amount", request.getAmount()); // Amount in paise
            orderJson.put("currency", request.getCurrency() != null ? request.getCurrency() : "INR");
            orderJson.put("receipt", request.getReceipt() != null ? request.getReceipt() : generateReceipt(request.getUserId()));

            // NOTE: Do NOT send customer details to Razorpay API
            // Razorpay API does not accept customer_email, customer_phone, customer_name fields
            // We store these in our database and use them for internal email notifications after payment verification

            // Add notes if provided
            if (request.getNotes() != null) {
                JSONObject notesJson = new JSONObject();
                notesJson.put("notes", request.getNotes());
                orderJson.put("notes", notesJson);
            }

            if (request.getDescription() != null) {
                orderJson.put("description", request.getDescription());
            }

            // Create order with Razorpay
            Order order = razorpayClient.orders.create(orderJson);
            String orderId = order.get("id");

            logger.info("Order created successfully with orderId: {}", orderId);

            // Save payment record in database
            PaymentEntity paymentEntity = PaymentEntity.builder()
                    .userId(request.getUserId())
                    .razorpayOrderId(orderId)
                    .amount(BigDecimal.valueOf(request.getAmount()))
                    .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                    .description(request.getDescription())
                    .receipt(request.getReceipt() != null ? request.getReceipt() : generateReceipt(request.getUserId()))
                    .status(PaymentEntity.PaymentStatus.PENDING)
                    .customerEmail(customerEmail)
                    .customerPhone(request.getCustomerPhone())
                    .customerName(request.getCustomerName())
                    .notes(request.getNotes())
                    .build();

            paymentRepository.save(paymentEntity);
            logger.debug("Payment record saved in database");

            return CreateOrderResponse.builder()
                    .razorpayOrderId(orderId)
                    .amount(request.getAmount())
                    .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                    .status("PENDING")
                    .message("Order created successfully. Complete payment on frontend.")
                    .build();

        } catch (Exception e) {
            logger.error("Error creating payment order: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create payment order: " + e.getMessage(), e);
        }
    }

    /**
     * Verify payment after successful transaction
     */
    public PaymentResponse verifyPayment(PaymentVerificationRequest request) {
        try {
            logger.debug("Verifying payment - orderId: {}, paymentId: {}", request.getRazorpayOrderId(), request.getRazorpayPaymentId());

            // Verify signature
            String signature = verifyPaymentSignature(
                    request.getRazorpayOrderId(),
                    request.getRazorpayPaymentId(),
                    request.getRazorpaySignature()
            );

            if (!signature.equals(request.getRazorpaySignature())) {
                logger.warn("Signature verification failed for orderId: {}", request.getRazorpayOrderId());
                throw new RuntimeException("Payment verification failed: Invalid signature");
            }

            logger.info("Signature verified successfully");

            // Update payment record
            Optional<PaymentEntity> paymentOpt = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId());

            if (paymentOpt.isEmpty()) {
                logger.error("Payment record not found for orderId: {}", request.getRazorpayOrderId());
                throw new RuntimeException("Payment record not found");
            }

            PaymentEntity payment = paymentOpt.get();
            payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
            payment.setRazorpaySignature(request.getRazorpaySignature());
            payment.setStatus(PaymentEntity.PaymentStatus.CAPTURED);

            // Fetch payment details from Razorpay and update
            try {
                com.razorpay.Payment razorpayPayment = razorpayClient.payments.fetch(request.getRazorpayPaymentId());
                payment.setPaymentMethod(razorpayPayment.get("method"));
            } catch (Exception e) {
                logger.warn("Could not fetch payment method from Razorpay: {}", e.getMessage());
            }

            paymentRepository.save(payment);
            logger.info("Payment verified and updated successfully - paymentId: {}", request.getRazorpayPaymentId());

            return PaymentResponse.fromEntity(payment);

        } catch (Exception e) {
            logger.error("Error verifying payment: {}", e.getMessage(), e);

            // Update payment status to FAILED
            try {
                Optional<PaymentEntity> paymentOpt = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId());
                if (paymentOpt.isPresent()) {
                    PaymentEntity payment = paymentOpt.get();
                    payment.setStatus(PaymentEntity.PaymentStatus.FAILED);
                    payment.setErrorMessage(e.getMessage());
                    paymentRepository.save(payment);
                }
            } catch (Exception saveError) {
                logger.error("Failed to update payment status to FAILED: {}", saveError.getMessage());
            }

            throw new RuntimeException("Payment verification failed: " + e.getMessage(), e);
        }
    }

    /**
     * Verify payment signature using HMAC-SHA256
     */
    private String verifyPaymentSignature(String orderId, String paymentId, String signature) throws Exception {
        String data = orderId + "|" + paymentId;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(), "HmacSHA256"));
        byte[] hashBytes = mac.doFinal(data.getBytes());
        return HexFormat.of().formatHex(hashBytes);
    }

    /**
     * Get payment details by order ID
     */
    public PaymentResponse getPaymentByOrderId(String orderId) {
        try {
            logger.debug("Fetching payment details for orderId: {}", orderId);
            Optional<PaymentEntity> payment = paymentRepository.findByRazorpayOrderId(orderId);

            if (payment.isEmpty()) {
                logger.warn("Payment not found for orderId: {}", orderId);
                throw new RuntimeException("Payment not found");
            }

            return PaymentResponse.fromEntity(payment.get());
        } catch (Exception e) {
            logger.error("Error fetching payment: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch payment: " + e.getMessage(), e);
        }
    }

    /**
     * Get payment details by payment ID
     */
    public PaymentResponse getPaymentByPaymentId(String paymentId) {
        try {
            logger.debug("Fetching payment details for paymentId: {}", paymentId);
            Optional<PaymentEntity> payment = paymentRepository.findByRazorpayPaymentId(paymentId);

            if (payment.isEmpty()) {
                logger.warn("Payment not found for paymentId: {}", paymentId);
                throw new RuntimeException("Payment not found");
            }

            return PaymentResponse.fromEntity(payment.get());
        } catch (Exception e) {
            logger.error("Error fetching payment: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch payment: " + e.getMessage(), e);
        }
    }

    /**
     * Get all payments for a user
     */
    public List<PaymentResponse> getUserPayments(String userId) {
        try {
            logger.debug("Fetching payments for userId: {}", userId);
            List<PaymentEntity> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
            return payments.stream()
                    .map(PaymentResponse::fromEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching user payments: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch user payments: " + e.getMessage(), e);
        }
    }

    /**
     * Refund a payment
     */
    public PaymentResponse refundPayment(String paymentId, Long refundAmount, String reason) {
        try {
            logger.debug("Refunding payment - paymentId: {}, amount: {}", paymentId, refundAmount);

            Optional<PaymentEntity> paymentOpt = paymentRepository.findByRazorpayPaymentId(paymentId);

            if (paymentOpt.isEmpty()) {
                logger.error("Payment not found for paymentId: {}", paymentId);
                throw new RuntimeException("Payment not found");
            }

            JSONObject refundJson = new JSONObject();
            if (refundAmount != null) {
                refundJson.put("amount", refundAmount); // Amount in paise
            }
            if (reason != null) {
                refundJson.put("notes", reason);
            }

            razorpayClient.payments.refund(paymentId, refundJson);
            logger.info("Refund initiated for paymentId: {}", paymentId);

            // Update payment status
            PaymentEntity payment = paymentOpt.get();
            payment.setStatus(PaymentEntity.PaymentStatus.REFUNDED);
            payment.setErrorMessage("Refund requested - Reason: " + (reason != null ? reason : "No reason provided"));
            paymentRepository.save(payment);

            return PaymentResponse.fromEntity(payment);

        } catch (Exception e) {
            logger.error("Error refunding payment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to refund payment: " + e.getMessage(), e);
        }
    }

    /**
     * Generate receipt for payment (max 40 characters for Razorpay)
     */
    private String generateReceipt(String userId) {
        // Generate a short receipt ID that won't exceed 40 characters
        // Format: RCP{timestamp8}-{hash4} (max ~20 chars)
        long timestamp = System.currentTimeMillis();
        String shortTimestamp = String.valueOf(timestamp % 100000000); // Last 8 digits
        int userIdHash = Math.abs(userId.hashCode()) % 10000; // 4-digit hash
        return "RCP" + shortTimestamp + "-" + String.format("%04d", userIdHash);
    }

    /**
     * Get Razorpay Key ID for frontend
     */
    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }
}

