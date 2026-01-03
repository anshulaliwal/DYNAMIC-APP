package com.saasapp.dynamic_app.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@Slf4j
public class QRCodeService {

    public byte[] generateQRCode(String data, int width, int height) throws WriterException, IOException {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, width, height);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            log.error("Error generating QR code", e);
            throw e;
        }
    }

    public String buildPaymentQRCodeData(String transactionId, Double amount, String currency) {
        return String.format("PAYMENT|TXN:%s|AMT:%s|CUR:%s|DATE:%s",
            transactionId,
            amount,
            currency,
            System.currentTimeMillis());
    }
}

