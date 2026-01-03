package com.saasapp.dynamic_app.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.saasapp.dynamic_app.dto.PaymentPdfDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class PdfService {

    public byte[] generatePaymentPdf(PaymentPdfDTO paymentDto) {
        try {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, outputStream);

            document.open();

            // Header with enhanced styling
            Paragraph header = new Paragraph("PAYMENT RECEIPT", new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD));
            header.setAlignment(Element.ALIGN_CENTER);
            header.setSpacingBefore(10);
            header.setSpacingAfter(5);
            document.add(header);

            // Separator line
            Paragraph separator = new Paragraph("_".repeat(50));
            separator.setAlignment(Element.ALIGN_CENTER);
            separator.setSpacingAfter(10);
            document.add(separator);

            // Company/System Details
            Paragraph companyDetails = new Paragraph("DynamicApp Payment System", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD));
            companyDetails.setAlignment(Element.ALIGN_CENTER);
            companyDetails.setSpacingAfter(10);
            document.add(companyDetails);


            // Create a table with QR code on the right
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{70, 30});

            // Left side - Payment Details Table
            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            detailsTable.setSpacingBefore(5);
            detailsTable.setSpacingAfter(5);

            addTableRow(detailsTable, "Transaction ID:", paymentDto.getTransactionId());
            addTableRow(detailsTable, "Payment ID:", paymentDto.getPaymentId() != null ? paymentDto.getPaymentId().toString() : "N/A");
            addTableRow(detailsTable, "User Name:", paymentDto.getUserName());
            addTableRow(detailsTable, "User Email:", paymentDto.getUserEmail());
            addTableRow(detailsTable, "Amount:", (paymentDto.getAmount() != null ? paymentDto.getAmount() : 0) + " " + (paymentDto.getCurrency() != null ? paymentDto.getCurrency() : "INR"));
            addTableRow(detailsTable, "Status:", paymentDto.getPaymentStatus() != null ? paymentDto.getPaymentStatus() : "PENDING");
            addTableRow(detailsTable, "Date:", paymentDto.getPaymentDate() != null ? paymentDto.getPaymentDate() : "N/A");
            addTableRow(detailsTable, "Description:", paymentDto.getDescription() != null ? paymentDto.getDescription() : "N/A");

            PdfPCell detailsCell = new PdfPCell(detailsTable);
            detailsCell.setBorder(PdfPCell.RECTANGLE);
            detailsCell.setBorderColor(BaseColor.LIGHT_GRAY);
            detailsCell.setPadding(10);
            detailsCell.setBackgroundColor(new BaseColor(245, 245, 245));
            headerTable.addCell(detailsCell);

            // Right side - QR Code (embedded directly in PDF only)
            PdfPCell qrCell = generateQRCodeCell(paymentDto);
            headerTable.addCell(qrCell);

            document.add(headerTable);

            document.add(new Paragraph(" "));

            // Separator line
            Paragraph footerSeparator = new Paragraph("_".repeat(50));
            footerSeparator.setAlignment(Element.ALIGN_CENTER);
            footerSeparator.setSpacingBefore(10);
            footerSeparator.setSpacingAfter(10);
            document.add(footerSeparator);

            // Footer
            Paragraph footer = new Paragraph("Thank you for your payment!", new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD));
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingAfter(5);
            document.add(footer);

            Paragraph footerDate = new Paragraph("Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), new Font(Font.FontFamily.HELVETICA, 9));
            footerDate.setAlignment(Element.ALIGN_CENTER);
            document.add(footerDate);

            document.close();

            return outputStream.toByteArray();
        } catch (DocumentException e) {
            log.error("Error generating PDF", e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    /**
     * Generate QR code cell for PDF embedding
     */
    private PdfPCell generateQRCodeCell(PaymentPdfDTO paymentDto) {
        try {
            log.debug("Starting QR code generation for transaction: {}", paymentDto.getTransactionId());

            String qrData = buildQRCodeData(paymentDto);
            log.debug("QR data built: {}", qrData);

            byte[] qrCodeImage = generateQRCodeImage(qrData, 150, 150);
            log.debug("QR code image generated, size: {} bytes", qrCodeImage.length);

            Image qrImage = Image.getInstance(qrCodeImage);
            qrImage.scaleToFit(120, 120);
            qrImage.setAlignment(Image.ALIGN_CENTER);

            PdfPCell qrImageCell = new PdfPCell();
            qrImageCell.addElement(qrImage);
            qrImageCell.setBorder(PdfPCell.RECTANGLE);
            qrImageCell.setBorderColor(BaseColor.BLACK);
            qrImageCell.setPadding(8);
            qrImageCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qrImageCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            qrImageCell.setBackgroundColor(BaseColor.WHITE);

            // Add "QR Code" label with scan instruction
            Paragraph qrLabel = new Paragraph("QR Code", new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD));
            qrLabel.setAlignment(Element.ALIGN_CENTER);
            PdfPCell labelCell = new PdfPCell(qrLabel);
            labelCell.setBorder(PdfPCell.NO_BORDER);
            labelCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            labelCell.setPaddingBottom(5);

            // Add scan instruction
            Paragraph scanHint = new Paragraph("Scan for verification", new Font(Font.FontFamily.HELVETICA, 8));
            scanHint.setAlignment(Element.ALIGN_CENTER);
            PdfPCell scanCell = new PdfPCell(scanHint);
            scanCell.setBorder(PdfPCell.NO_BORDER);
            scanCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            scanCell.setPaddingTop(5);

            // Create QR container with white background and proper border
            PdfPTable qrTable = new PdfPTable(1);
            qrTable.setWidthPercentage(100);

            qrTable.addCell(labelCell);
            qrTable.addCell(qrImageCell);
            qrTable.addCell(scanCell);

            PdfPCell qrTableCell = new PdfPCell(qrTable);
            qrTableCell.setBorder(PdfPCell.NO_BORDER);
            qrTableCell.setPadding(5);

            log.info("QR code embedded successfully in PDF for transaction: {}", paymentDto.getTransactionId());
            return qrTableCell;

        } catch (BadElementException e) {
            log.error("BadElementException while embedding QR code: {}", e.getMessage(), e);
            return createFailureCell("QR Code\nGeneration\nFailed");
        } catch (Exception e) {
            log.error("Exception while embedding QR code: {}", e.getMessage(), e);
            return createFailureCell("QR Code\nGeneration\nFailed");
        }
    }

    /**
     * Create a failure cell when QR code generation fails
     */
    private PdfPCell createFailureCell(String message) {
        PdfPCell emptyCell = new PdfPCell(new Paragraph(message));
        emptyCell.setBorder(PdfPCell.RECTANGLE);
        emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        emptyCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return emptyCell;
    }

    /**
     * Generate QR code image from payment data
     * QR code is embedded only in PDF, not exposed separately
     */
    private byte[] generateQRCodeImage(String data, int width, int height) throws WriterException {
        try {
            if (data == null || data.isEmpty()) {
                throw new IllegalArgumentException("QR data cannot be null or empty");
            }

            log.debug("Generating QR code with data: {}, dimensions: {}x{}", data, width, height);

            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, width, height);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            byte[] imageBytes = outputStream.toByteArray();
            log.debug("QR code image generated successfully, size: {} bytes", imageBytes.length);

            return imageBytes;
        } catch (WriterException e) {
            log.error("WriterException: Error generating QR code image for PDF embedding", e);
            throw e;
        } catch (Exception e) {
            log.error("Exception: Error generating QR code image for PDF embedding: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    /**
     * Build QR code data from payment information
     * QR code contains scannable payment verification details with URL redirect
     */
    private String buildQRCodeData(PaymentPdfDTO paymentDto) {
        String transactionId = paymentDto.getTransactionId() != null ? paymentDto.getTransactionId() : "UNKNOWN";
        String paymentId = paymentDto.getPaymentId() != null ? paymentDto.getPaymentId().toString() : "N/A";
        String amount = paymentDto.getAmount() != null ? paymentDto.getAmount().toString() : "0";
        String currency = paymentDto.getCurrency() != null ? paymentDto.getCurrency() : "INR";
        String date = paymentDto.getPaymentDate() != null ? paymentDto.getPaymentDate() : "N/A";
        String userName = paymentDto.getUserName() != null ? paymentDto.getUserName() : "UNKNOWN";
        String email = paymentDto.getUserEmail() != null ? paymentDto.getUserEmail() : "N/A";
        String status = paymentDto.getPaymentStatus() != null ? paymentDto.getPaymentStatus() : "PENDING";
        String description = paymentDto.getDescription() != null ? paymentDto.getDescription() : "Payment Receipt";

        // Build the complete URL with encoded query parameters
        // The data parameter contains the payment info as key=value pairs
        String dataParam = String.format(
            "txn=%s&pid=%s&amt=%s&cur=%s&status=%s&date=%s&user=%s&email=%s&desc=%s",
            transactionId, paymentId, amount, currency, status, date, userName, email, description
        );

        // URL encode the entire data parameter value
        String encodedData = encodeURIComponent(dataParam);

        // Use local IP address (172.20.10.2) for mobile device access
        // For production, change to your production domain: https://yourdomain.com
        return "http://172.20.10.2:8080/api/payment/verify-qr?data=" + encodedData;
    }

    /**
     * URL encode helper method
     */
    private String encodeURIComponent(String component) {
        try {
            return java.net.URLEncoder.encode(component, java.nio.charset.StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            log.warn("Error encoding URI component: {}", e.getMessage());
            return component;
        }
    }

    private void addTableRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
        labelCell.setBackgroundColor(new BaseColor(220, 220, 220));
        labelCell.setPadding(6);
        labelCell.setBorderColor(new BaseColor(200, 200, 200));
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "N/A", new Font(Font.FontFamily.HELVETICA, 10)));
        valueCell.setPadding(6);
        valueCell.setBackgroundColor(BaseColor.WHITE);
        valueCell.setBorderColor(new BaseColor(200, 200, 200));
        table.addCell(valueCell);
    }
}
