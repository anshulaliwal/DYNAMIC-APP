package com.saasapp.dynamic_app.controller;

import com.saasapp.dynamic_app.dto.*;
import com.saasapp.dynamic_app.service.EmailService;
import com.saasapp.dynamic_app.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = {"https://nyayapathlegal.in", "https://client-ca-saas-app.vercel.app", "https://super-saas-app.vercel.app", "http://localhost:3000", "http://localhost:3001"},
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
        allowCredentials = "true",
        maxAge = 3600)
public class PaymentController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;


    @Autowired
    private EmailService emailService;

    /**
     * Create a payment order
     * POST /api/payment/create-order
     */
    @PostMapping("/create-order")
    public ResponseEntity<?> createPaymentOrder(@RequestBody CreatePaymentRequest request) {
        try {
            logger.debug("Received payment order request for userId: {}, amount: {}", request.getUserId(), request.getAmount());

            // Validate request
            if (request.getUserId() == null || request.getUserId().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("userId is required"));
            }

            if (request.getAmount() == null || request.getAmount() <= 0) {
                return ResponseEntity.badRequest().body(createErrorResponse("amount must be greater than 0"));
            }

            CreateOrderResponse response = paymentService.createPaymentOrder(request);
            logger.info("Payment order created successfully - orderId: {}", response.getRazorpayOrderId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error creating payment order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to create payment order: " + e.getMessage()));
        }
    }

    /**
     * Verify payment after successful transaction
     * POST /api/payment/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody PaymentVerificationRequest request) {
        try {
            logger.debug("Received payment verification request for orderId: {}", request.getRazorpayOrderId());

            // Validate request
            if (request.getRazorpayOrderId() == null || request.getRazorpayOrderId().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("razorpayOrderId is required"));
            }

            if (request.getRazorpayPaymentId() == null || request.getRazorpayPaymentId().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("razorpayPaymentId is required"));
            }

            if (request.getRazorpaySignature() == null || request.getRazorpaySignature().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("razorpaySignature is required"));
            }

            PaymentResponse response = paymentService.verifyPayment(request);
            logger.info("Payment verified successfully - paymentId: {}", request.getRazorpayPaymentId());
            logger.info("Payment status: {}", response.getStatus());
            logger.info("Customer email: {}", response.getCustomerEmail());

            // Send payment receipt email asynchronously if payment is successful (CAPTURED or SUCCESS status)
            if ("CAPTURED".equalsIgnoreCase(response.getStatus()) || "SUCCESS".equalsIgnoreCase(response.getStatus())) {
                logger.info(">>> PAYMENT VERIFIED: Status is {}, triggering email send", response.getStatus());
                try {
                    logger.info(">>> EMAIL SERVICE: About to call sendPaymentReceiptEmail method");
                    sendPaymentReceiptEmail(response);
                    logger.info(">>> EMAIL SERVICE: sendPaymentReceiptEmail method called successfully");
                } catch (Exception e) {
                    logger.warn(">>> EMAIL SERVICE FAILED: Failed to send payment receipt email, but payment was verified: {}", e.getMessage(), e);
                    // Don't fail the payment verification if email fails
                }
            } else {
                logger.info(">>> PAYMENT NOT SUCCESSFUL: Status is {}, skipping email send", response.getStatus());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error verifying payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Payment verification failed: " + e.getMessage()));
        }
    }

    /**
     * Get payment details by order ID
     * GET /api/payment/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getPaymentByOrderId(@PathVariable String orderId) {
        try {
            logger.debug("Fetching payment for orderId: {}", orderId);
            PaymentResponse response = paymentService.getPaymentByOrderId(orderId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching payment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Payment not found: " + e.getMessage()));
        }
    }

    /**
     * Get payment details by payment ID
     * GET /api/payment/payment/{paymentId}
     */
    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<?> getPaymentByPaymentId(@PathVariable String paymentId) {
        try {
            logger.debug("Fetching payment for paymentId: {}", paymentId);
            PaymentResponse response = paymentService.getPaymentByPaymentId(paymentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching payment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Payment not found: " + e.getMessage()));
        }
    }

    /**
     * Get all payments for a user
     * GET /api/payment/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserPayments(@PathVariable String userId) {
        try {
            logger.debug("Fetching payments for userId: {}", userId);
            List<PaymentResponse> responses = paymentService.getUserPayments(userId);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            logger.error("Error fetching user payments: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch user payments: " + e.getMessage()));
        }
    }

    /**
     * Refund a payment
     * POST /api/payment/refund
     */
    @PostMapping("/refund")
    public ResponseEntity<?> refundPayment(
            @RequestParam String paymentId,
            @RequestParam(required = false) Long refundAmount,
            @RequestParam(required = false, defaultValue = "Refund requested by user") String reason) {
        try {
            logger.debug("Refunding payment - paymentId: {}, amount: {}", paymentId, refundAmount);

            if (paymentId == null || paymentId.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("paymentId is required"));
            }

            PaymentResponse response = paymentService.refundPayment(paymentId, refundAmount, reason);
            logger.info("Payment refunded successfully - paymentId: {}", paymentId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error refunding payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to refund payment: " + e.getMessage()));
        }
    }

    /**
     * Get Razorpay Key ID for frontend integration
     * GET /api/payment/key
     */
    @GetMapping("/key")
    public ResponseEntity<?> getRazorpayKeyId() {
        try {
            String keyId = paymentService.getRazorpayKeyId();
            Map<String, String> response = new HashMap<>();
            response.put("keyId", keyId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching Razorpay key: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch Razorpay key: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint for payment service
     * GET /api/payment/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "Payment service is running");
        response.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * Test email endpoint to verify email configuration
     * POST /api/payment/test-email
     * Body: { "email": "test@example.com" }
     */
    @PostMapping("/test-email")
    public ResponseEntity<?> testEmail(@RequestBody Map<String, String> request) {
        try {
            logger.info(">>> TEST EMAIL: Received test email request");
            String testEmail = request.get("email");

            if (testEmail == null || testEmail.isEmpty()) {
                logger.error(">>> TEST EMAIL: Email address is required");
                return ResponseEntity.badRequest().body(createErrorResponse("email parameter is required"));
            }

            logger.info(">>> TEST EMAIL: Preparing test email request for: {}", testEmail);

            PaymentEmailRequest emailRequest = PaymentEmailRequest.builder()
                    .transactionId("TEST-" + System.currentTimeMillis())
                    .email(testEmail)
                    .userName("Test User")
                    .amount(999.99)
                    .currency("INR")
                    .date(LocalDateTime.now().toString())
                    .description("This is a test payment receipt email")
                    .paymentId(1L)
                    .status("SUCCESS")
                    .build();

            logger.info(">>> TEST EMAIL: Calling sendPaymentReceiptEmailAsync()");
            emailService.sendPaymentReceiptEmailAsync(emailRequest);
            logger.info(">>> TEST EMAIL: Test email request submitted successfully");

            Map<String, String> response = new HashMap<>();
            response.put("message", "Test email submitted. Check your inbox shortly.");
            response.put("email", testEmail);
            response.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error(">>> TEST EMAIL: Failed to send test email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to send test email: " + e.getMessage()));
        }
    }


    /**
     * QR Code Verification Page - Display scanned QR data with beautiful UI
     * GET /api/payment/verify-qr
     * Query params: data={encoded_qr_data}
     */
    @GetMapping("/verify-qr")
    public ResponseEntity<String> verifyQRCode() {
        try {
            logger.debug("Serving QR verification page");

            // Read the HTML file from static resources
            String htmlContent = readHtmlFile();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);

            return new ResponseEntity<>(htmlContent, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error serving verification page: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.TEXT_PLAIN)
                .body("Error loading verification page: " + e.getMessage());
        }
    }

    /**
     * Read HTML file from static resources
     */
    private String readHtmlFile() throws Exception {
        try (var inputStream = this.getClass().getClassLoader()
                .getResourceAsStream("static/verify-qr.html")) {
            if (inputStream == null) {
                throw new RuntimeException("verify-qr.html not found in static resources");
            }
            return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /**
     * Create error response object
     */
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        response.put("timestamp", java.time.Instant.now().toString());
        return response;
    }

    /**
     * Send payment receipt email asynchronously
     * This method constructs the PaymentEmailRequest from PaymentResponse
     * and triggers async email sending with PDF attachment
     *
     * @param paymentResponse Payment details from verification
     */
    private void sendPaymentReceiptEmail(PaymentResponse paymentResponse) {
        try {
            logger.info(">>> EMAIL PREPARATION: Starting email preparation for payment: {}", paymentResponse.getRazorpayPaymentId());
            logger.debug(">>> EMAIL PREPARATION: Payment response status: {}", paymentResponse.getStatus());
            logger.debug(">>> EMAIL PREPARATION: Customer email: {}", paymentResponse.getCustomerEmail());
            logger.debug(">>> EMAIL PREPARATION: Customer name: {}", paymentResponse.getCustomerName());

            // Check if customer email is available
            String emailToUse = paymentResponse.getCustomerEmail();
            if (emailToUse == null || emailToUse.isEmpty()) {
                logger.warn(">>> EMAIL PREPARATION WARNING: Customer email is null or empty!");
                // Try to use userId as fallback if it looks like an email
                if (paymentResponse.getUserId() != null && paymentResponse.getUserId().contains("@")) {
                    logger.info(">>> EMAIL PREPARATION: Using userId as email: {}", paymentResponse.getUserId());
                    emailToUse = paymentResponse.getUserId();
                } else {
                    logger.error(">>> EMAIL PREPARATION FAILED: No valid email found - customer email is null and userId is not an email!");
                    throw new RuntimeException("Customer email is required for sending receipt. Please provide customerEmail in create-order request.");
                }
            }

            PaymentEmailRequest emailRequest = PaymentEmailRequest.builder()
                    .transactionId(paymentResponse.getRazorpayPaymentId())
                    .email(emailToUse)
                    .userName(paymentResponse.getCustomerName() != null ? paymentResponse.getCustomerName() : paymentResponse.getUserId())
                    .amount(paymentResponse.getAmount().doubleValue())
                    .currency(paymentResponse.getCurrency())
                    .date(LocalDateTime.now().toString())
                    .description(paymentResponse.getDescription())
                    .paymentId(paymentResponse.getId())
                    .status(paymentResponse.getStatus())
                    .build();

            logger.info(">>> EMAIL PREPARATION: EmailRequest constructed successfully");
            logger.debug(">>> EMAIL PREPARATION: Email destination: {}", emailRequest.getEmail());
            logger.debug(">>> EMAIL PREPARATION: Transaction ID: {}", emailRequest.getTransactionId());

            logger.info(">>> ASYNC EMAIL TRIGGER: Calling emailService.sendPaymentReceiptEmailAsync()");
            emailService.sendPaymentReceiptEmailAsync(emailRequest);
            logger.info(">>> ASYNC EMAIL TRIGGER: sendPaymentReceiptEmailAsync() call completed (method is async, execution may continue in background)");

        } catch (Exception e) {
            logger.error(">>> EMAIL PREPARATION FAILED: Error preparing payment receipt email: {}", e.getMessage(), e);
            logger.error(">>> EMAIL PREPARATION FAILED: Exception type: {}, Cause: {}", e.getClass().getName(), e.getCause());
            throw new RuntimeException("Failed to prepare payment receipt email", e);
        }
    }
}


