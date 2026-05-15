package ro.ophthacloud.shared.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Generates QR code PNG images using ZXing.
 * Used for prescription verification URLs per GUIDE_06 §5.2.
 */
@Slf4j
@Component
public class QrCodeGenerator {

    private static final int DEFAULT_SIZE = 300;

    /**
     * Generates a QR code PNG image containing the given text content.
     *
     * @param content  the text to encode (e.g. verification URL)
     * @param widthPx  image width in pixels
     * @param heightPx image height in pixels
     * @return PNG image bytes
     */
    public byte[] generatePng(String content, int widthPx, int heightPx) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, widthPx, heightPx);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);
            return baos.toByteArray();
        } catch (WriterException | IOException e) {
            log.error("Failed to generate QR code for content length={}", content.length(), e);
            throw new RuntimeException("QR code generation failed", e);
        }
    }

    /**
     * Generates a 300×300 QR code PNG (default size per GUIDE_06 §5.2).
     */
    public byte[] generatePng(String content) {
        return generatePng(content, DEFAULT_SIZE, DEFAULT_SIZE);
    }
}
