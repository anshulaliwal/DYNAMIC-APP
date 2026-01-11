package com.saasapp.dynamic_app.service.impl;

import com.saasapp.dynamic_app.dto.PaymentEmailRequest;
import com.saasapp.dynamic_app.exception.EmailSendingException;
import com.saasapp.dynamic_app.service.EmailService;
import com.saasapp.dynamic_app.service.PdfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Email Service Implementation
 * Handles email sending with PDF attachment using Spring Mail
 * Supports both synchronous and asynchronous email delivery
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PdfService pdfService;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.support}")
    private String supportEmail;

    /**
     * Send OTP email for email verification
     */
    @Override
    public void sendOtpEmail(String email, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Email Verification - Your OTP");

            String emailContent = String.format(
                """
                <!DOCTYPE html>
                <html>
                    <head>
                        <meta charset="UTF-8">
                        <style>
                            body { font-family: Arial, sans-serif; color: #333; }
                            .container { max-width: 600px; margin: 0 auto; background: #f5f5f5; padding: 20px; border-radius: 8px; }
                            .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 20px; text-align: center; border-radius: 8px; }
                            .content { background: white; padding: 30px; margin-top: 20px; border-radius: 8px; }
                            .otp-box { background: #f0f0f0; padding: 20px; text-align: center; border-radius: 8px; margin: 20px 0; border: 2px solid #667eea; }
                            .otp-code { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 5px; }
                            .footer { text-align: center; color: #999; font-size: 12px; margin-top: 20px; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <div class="header">
                                <h1>Email Verification</h1>
                            </div>
                            <div class="content">
                                <p>Hello,</p>
                                <p>Your OTP for email verification is:</p>
                                <div class="otp-box">
                                    <div class="otp-code">%s</div>
                                </div>
                                <p style="color: #666;">This OTP will expire in 10 minutes.</p>
                                <p style="color: #999; font-size: 14px;">If you didn't request this OTP, please ignore this email.</p>
                            </div>
                            <div class="footer">
                                <p>DynamicApp | Automated Email</p>
                            </div>
                        </div>
                    </body>
                </html>
                """,
                otp
            );

            helper.setText(emailContent, true);
            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    /**
     * Send welcome email after successful signup
     */
    @Override
    public void sendWelcomeEmail(String email, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Welcome to Dynamic App!");

            String emailContent = String.format(
                """
                <!DOCTYPE html>
                <html>
                    <head>
                        <meta charset="UTF-8">
                        <style>
                            body { font-family: Arial, sans-serif; color: #333; }
                            .container { max-width: 600px; margin: 0 auto; background: #f5f5f5; padding: 20px; border-radius: 8px; }
                            .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 20px; text-align: center; border-radius: 8px; }
                            .content { background: white; padding: 30px; margin-top: 20px; border-radius: 8px; }
                            .footer { text-align: center; color: #999; font-size: 12px; margin-top: 20px; }
                            .button { background: #667eea; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block; margin-top: 20px; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <div class="header">
                                <h1>Welcome!</h1>
                            </div>
                            <div class="content">
                                <p>Hello %s,</p>
                                <p>Welcome to Dynamic App!</p>
                                <p>Your email has been verified and your account is now active.</p>
                                <p>You can now log in and start using our platform.</p>
                                <a href="http://localhost:3000/login" class="button">Log In Now</a>
                                <p style="color: #999; font-size: 14px; margin-top: 30px;">Thank you for signing up!</p>
                            </div>
                            <div class="footer">
                                <p>DynamicApp | Automated Email</p>
                            </div>
                        </div>
                    </body>
                </html>
                """,
                fullName
            );

            helper.setText(emailContent, true);
            mailSender.send(message);
            log.info("Welcome email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", email, e.getMessage());
        }
    }

    /**
     * Send payment receipt email synchronously
     * Logs success/failure and throws exception on failure
     */
    @Override
    public void sendPaymentReceiptEmail(PaymentEmailRequest request) {
        try {
            log.info("=== PAYMENT EMAIL SENDING INITIATED ===");
            log.info("Target email: {}", request.getEmail());
            log.info("Transaction ID: {}", request.getTransactionId());
            log.info("Customer name: {}", request.getUserName());
            log.info("Amount: {} {}", request.getAmount(), request.getCurrency());

            // Validate email request
            validateEmailRequest(request);
            log.debug("Email request validation passed");

            // Send the email
            sendEmail(request);

            log.info("✓ Payment receipt email sent successfully to: {}", request.getEmail());
            log.info("=== EMAIL SENDING COMPLETED SUCCESSFULLY ===");
        } catch (EmailSendingException e) {
            log.error("✗ Failed to send payment receipt email to: {}", request.getEmail(), e);
            throw e;
        } catch (Exception e) {
            log.error("✗ Unexpected error while sending payment receipt email to: {}", request.getEmail(), e);
            log.error("Exception type: {}", e.getClass().getName());
            log.error("Exception message: {}", e.getMessage());
            throw new EmailSendingException("Failed to send payment receipt email", e);
        }
    }

    /**
     * Send payment receipt email asynchronously
     * Non-blocking operation that executes in a separate thread
     */
    @Override
    @Async
    public void sendPaymentReceiptEmailAsync(PaymentEmailRequest request) {
        log.info(">>> ASYNC EMAIL THREAD STARTED for: {}", request.getEmail());
        log.debug("Processing async email sending request for: {}", request.getEmail());
        log.debug("Thread name: {}", Thread.currentThread().getName());
        try {
            sendPaymentReceiptEmail(request);
            log.info(">>> ASYNC EMAIL THREAD COMPLETED SUCCESSFULLY for: {}", request.getEmail());
        } catch (Exception e) {
            log.error(">>> ASYNC EMAIL THREAD FAILED for: {}", request.getEmail(), e);
        }
    }

    /**
     * Validate email request before sending
     *
     * @param request PaymentEmailRequest to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateEmailRequest(PaymentEmailRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("PaymentEmailRequest cannot be null");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email address is required");
        }
        if (request.getTransactionId() == null || request.getTransactionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID is required");
        }
        if (request.getUserName() == null || request.getUserName().trim().isEmpty()) {
            throw new IllegalArgumentException("User name is required");
        }
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
    }

    /**
     * Send email with PDF attachment
     *
     * @param request PaymentEmailRequest containing payment details
     * @throws MessagingException if email message creation/sending fails
     * @throws EmailSendingException if PDF generation fails
     */
    private void sendEmail(PaymentEmailRequest request) throws MessagingException {
        try {
            log.debug("[STEP 1] Creating MIME message...");
            // Create MIME message
            MimeMessage message = mailSender.createMimeMessage();
            log.debug("[STEP 1] MIME message created successfully");

            log.debug("[STEP 2] Setting up MIME message helper...");
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            log.debug("[STEP 2] MIME helper configured");

            log.debug("[STEP 3] Setting email headers - From: {}, To: {}", fromEmail, request.getEmail());
            // Set email headers
            helper.setFrom(fromEmail);
            helper.setTo(request.getEmail());
            helper.setSubject("Payment Receipt - Transaction ID: " + request.getTransactionId());
            log.debug("[STEP 3] Email headers set");

            log.debug("[STEP 4] Building email HTML content...");
            // Set HTML content
            helper.setText(buildEmailContent(request), true);
            log.debug("[STEP 4] Email HTML content added");

            log.debug("[STEP 5] Generating PDF attachment...");
            // Generate PDF from PdfService
            byte[] pdfContent = generatePaymentPdf(request);
            log.debug("[STEP 5] PDF generated successfully, size: {} bytes", pdfContent.length);

            log.debug("[STEP 6] Attaching PDF to email...");
            // Attach PDF
            String fileName = "Payment_Receipt_" + request.getTransactionId() + ".pdf";
            helper.addAttachment(
                fileName,
                () -> new java.io.ByteArrayInputStream(pdfContent),
                "application/pdf"
            );
            log.debug("[STEP 6] PDF attachment added: {}", fileName);

            log.debug("[STEP 7] Sending email via SMTP...");
            // Send email
            mailSender.send(message);
            log.debug("[STEP 7] Email sent successfully via SMTP");

        } catch (MessagingException e) {
            log.error("✗ MessagingException while sending email: {}", e.getMessage(), e);
            log.error("MessagingException details - Class: {}, Cause: {}", e.getClass().getName(), e.getCause());
            throw new EmailSendingException("Failed to create or send email message", e);
        } catch (Exception e) {
            log.error("✗ Exception while sending email: {}", e.getMessage(), e);
            log.error("Exception details - Class: {}, Cause: {}", e.getClass().getName(), e.getCause());
            throw new EmailSendingException("Failed to send email", e);
        }
    }

    /**
     * Generate payment PDF using PdfService
     *
     * @param request PaymentEmailRequest with payment details
     * @return byte array of PDF content
     * @throws EmailSendingException if PDF generation fails
     */
    private byte[] generatePaymentPdf(PaymentEmailRequest request) {
        try {
            log.debug("Generating PDF for transaction: {}", request.getTransactionId());

            // Create DTO for PdfService
            com.saasapp.dynamic_app.dto.PaymentPdfDTO pdfDto =
                com.saasapp.dynamic_app.dto.PaymentPdfDTO.builder()
                    .transactionId(request.getTransactionId())
                    .paymentId(request.getPaymentId())
                    .userName(request.getUserName())
                    .userEmail(request.getEmail())
                    .amount(request.getAmount())
                    .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                    .paymentStatus(request.getStatus() != null ? request.getStatus() : "SUCCESS")
                    .paymentDate(request.getDate() != null ? request.getDate() : LocalDateTime.now().toString())
                    .description(request.getDescription())
                    .build();

            byte[] pdfContent = pdfService.generatePaymentPdf(pdfDto);
            log.debug("PDF generated successfully, size: {} bytes", pdfContent.length);

            return pdfContent;
        } catch (Exception e) {
            log.error("Failed to generate PDF for transaction: {}", request.getTransactionId(), e);
            throw new EmailSendingException("Failed to generate payment PDF", e);
        }
    }

    /**
     * Build HTML email content with payment details
     *
     * @param request PaymentEmailRequest with payment details
     * @return HTML email content as string
     */
    private String buildEmailContent(PaymentEmailRequest request) {
        String formattedAmount = String.format("%.2f", request.getAmount());
        String currency = request.getCurrency() != null ? request.getCurrency() : "INR";
        String status = request.getStatus() != null ? request.getStatus() : "SUCCESS";
        String statusColor = status.equals("SUCCESS") ? "#28a745" :
                            status.equals("PENDING") ? "#ffc107" : "#dc3545";

        return String.format(
            """
            <!DOCTYPE html>
            <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            color: #333;
                            background-color: #f5f5f5;
                            margin: 0;
                            padding: 0;
                        }
                        .container {
                            max-width: 600px;
                            margin: 0 auto;
                            background-color: #ffffff;
                            border-radius: 8px;
                            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
                            overflow: hidden;
                        }
                        .header {
                            background: linear-gradient(135deg, #667eea 0%%,  #764ba2 100%%);
                            color: white;
                            padding: 30px;
                            text-align: center;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 28px;
                            font-weight: 600;
                        }
                        .content {
                            padding: 30px;
                        }
                        .greeting {
                            font-size: 16px;
                            margin-bottom: 20px;
                        }
                        .details-table {
                            width: 100%%;
                            border-collapse: collapse;
                            margin: 20px 0;
                        }
                        .details-table td {
                            padding: 12px;
                            border-bottom: 1px solid #eee;
                        }
                        .details-table .label {
                            font-weight: 600;
                            color: #555;
                            width: 40%%;
                        }
                        .details-table .value {
                            color: #333;
                        }
                        .status-badge {
                            display: inline-block;
                            background-color: %s;
                            color: white;
                            padding: 6px 12px;
                            border-radius: 4px;
                            font-weight: 600;
                            font-size: 14px;
                        }
                        .footer {
                            background-color: #f9f9f9;
                            padding: 20px;
                            text-align: center;
                            border-top: 1px solid #eee;
                            font-size: 13px;
                            color: #666;
                        }
                        .attachment-note {
                            background-color: #e8f5e9;
                            padding: 12px;
                            border-radius: 4px;
                            margin: 20px 0;
                            color: #2e7d32;
                            font-size: 14px;
                        }
                        .support-link {
                            color: #667eea;
                            text-decoration: none;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>✓ Payment Successful</h1>
                        </div>
                        <div class="content">
                            <p class="greeting">Dear %s,</p>
                            <p>Thank you for your payment. Your transaction has been completed successfully. Your receipt is attached to this email.</p>
                            
                            <table class="details-table">
                                <tr>
                                    <td class="label">Transaction ID:</td>
                                    <td class="value"><strong>%s</strong></td>
                                </tr>
                                <tr>
                                    <td class="label">Amount:</td>
                                    <td class="value"><strong>%s %s</strong></td>
                                </tr>
                                <tr>
                                    <td class="label">Date:</td>
                                    <td class="value">%s</td>
                                </tr>
                                <tr>
                                    <td class="label">Status:</td>
                                    <td class="value"><span class="status-badge">%s</span></td>
                                </tr>
                                <tr>
                                    <td class="label">Description:</td>
                                    <td class="value">%s</td>
                                </tr>
                            </table>
                            
                            <div class="attachment-note">
                                📎 Payment receipt PDF is attached to this email. Please keep it for your records.
                            </div>
                            
                            <p>If you have any questions or concerns about this transaction, please don't hesitate to contact our support team at <a href="mailto:%s" class="support-link">%s</a></p>
                            
                            <p>Best regards,<br><strong>Payment Team</strong></p>
                        </div>
                        <div class="footer">
                            <p>This is an automated email. Please do not reply to this email.</p>
                            <p>DynamicApp Payment System | Generated on: %s</p>
                        </div>
                    </div>
                </body>
            </html>
            """,
            statusColor,
            request.getUserName(),
            request.getTransactionId(),
            formattedAmount,
            currency,
            request.getDate(),
            status,
            request.getDescription(),
            supportEmail,
            supportEmail,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }
}

