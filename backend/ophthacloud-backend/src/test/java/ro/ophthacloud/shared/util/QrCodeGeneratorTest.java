package ro.ophthacloud.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link QrCodeGenerator}.
 *
 * Covers OC-043 acceptance criteria:
 * - generates non-empty byte array
 * - generated image dimension matches expected 300×300 px
 */
@DisplayName("QrCodeGenerator")
class QrCodeGeneratorTest {

    private final QrCodeGenerator generator = new QrCodeGenerator();

    @Test
    @DisplayName("generatePng: default size produces non-empty PNG byte array")
    void generatePng_default_shouldProduceNonEmptyBytes() {
        byte[] result = generator.generatePng("https://app.ophthacloud.ro/verify/test-token-123");
        assertThat(result).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("generatePng: default size produces 300×300 pixel image")
    void generatePng_default_shouldProduce300x300Image() throws Exception {
        byte[] result = generator.generatePng("https://app.ophthacloud.ro/verify/token");
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(result));
        assertThat(img).isNotNull();
        assertThat(img.getWidth()).isEqualTo(300);
        assertThat(img.getHeight()).isEqualTo(300);
    }

    @Test
    @DisplayName("generatePng: custom size produces image with requested dimensions")
    void generatePng_customSize_shouldProduceCorrectDimensions() throws Exception {
        byte[] result = generator.generatePng("https://example.com", 128, 128);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(result));
        assertThat(img).isNotNull();
        assertThat(img.getWidth()).isEqualTo(128);
        assertThat(img.getHeight()).isEqualTo(128);
    }

    @Test
    @DisplayName("generatePng: valid URL content encodes without error")
    void generatePng_withValidUrl_shouldSucceed() {
        // Any well-formed short content should encode successfully
        assertThat(generator.generatePng("https://ophthacloud.ro"))
                .isNotNull()
                .isNotEmpty();
    }
}
