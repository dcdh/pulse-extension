package com.damdamdeo.pulse.extension.query.deployment.file;

import com.damdamdeo.pulse.extension.core.query.file.ContentType;
import com.damdamdeo.pulse.extension.core.query.file.FileMetadata;
import com.damdamdeo.pulse.extension.core.query.file.ImageMetadataExtractorException;
import com.damdamdeo.pulse.extension.query.runtime.file.TikaImageMetadataExtractor;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class TikaImageMetadataExtractorTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addAsResource("facture.jpeg")
                    .addAsResource("facture.jpg")
                    .addAsResource("facture.pdf")
                    .addAsResource("facture.png"))
            .withConfigurationResource("application.properties");

    @Inject
    TikaImageMetadataExtractor tikaImageMetadataExtractor;

    @Test
    void shouldExtractJpgImageMetadata() {
        // Given
        try (final InputStream inputStream = this.getClass().getResourceAsStream("/facture.jpg")) {

            // When
            final FileMetadata extracted = tikaImageMetadataExtractor.extract(inputStream, ContentType.IMAGE_JPG);

            // Then
            assertAll(
                    () -> assertThat(extracted.metadata()).containsAllEntriesOf(Map.ofEntries(
                            Map.entry("Component 1", List.of(
                                    "Y component: Quantization table 0, Sampling factors 1 horiz/1 vert"
                            )),
                            Map.entry("Component 2", List.of(
                                    "Cb component: Quantization table 1, Sampling factors 1 horiz/1 vert"
                            )),
                            Map.entry("Component 3", List.of(
                                    "Cr component: Quantization table 1, Sampling factors 1 horiz/1 vert"
                            )),
                            Map.entry("Compression Type", List.of("Baseline")),
                            Map.entry("Content-Type", List.of("image/jpeg")),
                            Map.entry("Data Precision", List.of("8 bits")),
                            Map.entry("File Size", List.of("287759 bytes")),
                            Map.entry("Image Height", List.of("1491 pixels")),
                            Map.entry("Image Width", List.of("1055 pixels")),
                            Map.entry("Number of Components", List.of("3")),
                            Map.entry("Number of Tables", List.of("4 Huffman tables")),
                            Map.entry("Resolution Units", List.of("none")),
                            Map.entry("Thumbnail Height Pixels", List.of("0")),
                            Map.entry("Thumbnail Width Pixels", List.of("0")),
                            Map.entry("Version", List.of("1.1")),
                            Map.entry("X Resolution", List.of("1 dot")),
                            Map.entry("X-TIKA:Parsed-By", List.of(
                                    "org.apache.tika.parser.CompositeParser",
                                    "org.apache.tika.parser.image.JpegParser"
                            )),
                            Map.entry("X-TIKA:Parsed-By-Full-Set", List.of(
                                    "org.apache.tika.parser.CompositeParser",
                                    "org.apache.tika.parser.image.JpegParser"
                            )),
                            Map.entry("Y Resolution", List.of("1 dot")),
                            Map.entry("tiff:BitsPerSample", List.of("8")),
                            Map.entry("tiff:ImageLength", List.of("1491")),
                            Map.entry("tiff:ImageWidth", List.of("1055"))
                    )),
                    () -> assertThat(extracted.metadata()).containsKey("File Modified Date"),
                    () -> assertThat(extracted.metadata()).containsKey("File Name")
            );
        } catch (final IOException | ImageMetadataExtractorException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    void shouldExtractJpegImageMetadata() {
        // Given
        try (final InputStream inputStream = this.getClass().getResourceAsStream("/facture.jpeg")) {

            // When
            final FileMetadata extracted = tikaImageMetadataExtractor.extract(inputStream, ContentType.IMAGE_JPEG);

            // Then
            assertAll(
                    () -> assertThat(extracted.metadata()).containsAllEntriesOf(Map.ofEntries(
                            Map.entry("Resolution Units", List.of("none")),
                            Map.entry("Number of Tables", List.of("4 Huffman tables")),
                            Map.entry("Compression Type", List.of("Baseline")),
                            Map.entry("Data Precision", List.of("8 bits")),
                            Map.entry("X-TIKA:Parsed-By-Full-Set", List.of(
                                    "org.apache.tika.parser.CompositeParser",
                                    "org.apache.tika.parser.image.JpegParser"
                            )),
                            Map.entry("Number of Components", List.of("3")),
                            Map.entry("tiff:ImageLength", List.of("1491")),
                            Map.entry("Component 2", List.of(
                                    "Cb component: Quantization table 1, Sampling factors 1 horiz/1 vert"
                            )),
                            Map.entry("Thumbnail Height Pixels", List.of("0")),
                            Map.entry("Component 1", List.of(
                                    "Y component: Quantization table 0, Sampling factors 1 horiz/1 vert"
                            )),
                            Map.entry("Image Height", List.of("1491 pixels")),
                            Map.entry("Thumbnail Width Pixels", List.of("0")),
                            Map.entry("X Resolution", List.of("1 dot")),
                            Map.entry("Image Width", List.of("1055 pixels")),
                            Map.entry("Component 3", List.of(
                                    "Cr component: Quantization table 1, Sampling factors 1 horiz/1 vert"
                            )),
                            Map.entry("Version", List.of("1.1")),
                            Map.entry("X-TIKA:Parsed-By", List.of(
                                    "org.apache.tika.parser.CompositeParser",
                                    "org.apache.tika.parser.image.JpegParser"
                            )),
                            Map.entry("tiff:BitsPerSample", List.of("8")),
                            Map.entry("tiff:ImageWidth", List.of("1055")),
                            Map.entry("Content-Type", List.of("image/jpeg")),
                            Map.entry("Y Resolution", List.of("1 dot")),
                            Map.entry("File Size", List.of("287759 bytes"))
                    )),
                    () -> assertThat(extracted.metadata()).containsKey("File Modified Date"),
                    () -> assertThat(extracted.metadata()).containsKey("File Name")
            );
        } catch (final IOException | ImageMetadataExtractorException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    void shouldExtractPngImageMetadata() {
        // Given
        try (final InputStream inputStream = this.getClass().getResourceAsStream("/facture.png")) {

            // When
            final FileMetadata extracted = tikaImageMetadataExtractor.extract(inputStream, ContentType.IMAGE_PNG);

            // Then
            assertThat(extracted.metadata()).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                    Map.entry("imagereader:NumImages", List.of("1")),
                    Map.entry("Transparency Alpha", List.of("none")),
                    Map.entry("X-TIKA:Parsed-By-Full-Set", List.of(
                            "org.apache.tika.parser.CompositeParser",
                            "org.apache.tika.parser.image.ImageParser"
                    )),
                    Map.entry("tiff:ImageLength", List.of("1491")),
                    Map.entry("Compression CompressionTypeName", List.of("deflate")),
                    Map.entry("Dimension PixelAspectRatio", List.of("1.0")),
                    Map.entry("Data BitsPerSample", List.of("8 8 8")),
                    Map.entry("Data PlanarConfiguration", List.of("PixelInterleaved")),
                    Map.entry("IHDR", List.of(
                            "width=1055, height=1491, bitDepth=8, colorType=RGB, compressionMethod=deflate, filterMethod=adaptive, interlaceMethod=none"
                    )),
                    Map.entry("UnknownChunks UnknownChunk", List.of("caBX")),
                    Map.entry("Compression NumProgressiveScans", List.of("1")),
                    Map.entry("X-TIKA:Parsed-By", List.of(
                            "org.apache.tika.parser.CompositeParser",
                            "org.apache.tika.parser.image.ImageParser"
                    )),
                    Map.entry("Chroma ColorSpaceType", List.of("RGB")),
                    Map.entry("Chroma BlackIsZero", List.of("true")),
                    Map.entry("Compression Lossless", List.of("true")),
                    Map.entry("width", List.of("1055")),
                    Map.entry("Dimension ImageOrientation", List.of("Normal")),
                    Map.entry("tiff:BitsPerSample", List.of("8 8 8")),
                    Map.entry("tiff:ImageWidth", List.of("1055")),
                    Map.entry("Chroma NumChannels", List.of("3")),
                    Map.entry("Data SampleFormat", List.of("UnsignedIntegral")),
                    Map.entry("Content-Type", List.of("image/png")),
                    Map.entry("height", List.of("1491"))
            ));
        } catch (final IOException | ImageMetadataExtractorException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    void shouldExtractPdfImageMetadata() {
        // Given
        try (final InputStream inputStream = this.getClass().getResourceAsStream("/facture.pdf")) {

            // When
            final FileMetadata extracted = tikaImageMetadataExtractor.extract(inputStream, ContentType.APPLICATION_PDF);

            // Then
            assertThat(extracted.metadata()).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                    Map.entry("pdf:PDFVersion", List.of("1.3")),
                    Map.entry("pdf:docinfo:title", List.of("facture")),
                    Map.entry("xmp:CreatorTool", List.of("https://imagemagick.org")),
                    Map.entry("pdf:hasXFA", List.of("false")),
                    Map.entry("access_permission:modify_annotations", List.of("true")),
                    Map.entry("X-TIKA:Parsed-By-Full-Set", List.of(
                            "org.apache.tika.parser.CompositeParser",
                            "org.apache.tika.parser.pdf.PDFParser"
                    )),
                    Map.entry("dc:creator", List.of("https://imagemagick.org")),
                    Map.entry("dcterms:created", List.of("2026-08-04T19:54:59Z")),
                    Map.entry("dcterms:modified", List.of("2026-08-04T19:54:59Z")),
                    Map.entry("dc:format", List.of("application/pdf; version=1.3")),
                    Map.entry("pdf:docinfo:creator_tool", List.of("https://imagemagick.org")),
                    Map.entry("access_permission:fill_in_form", List.of("true")),
                    Map.entry("pdf:docinfo:modified", List.of("2026-08-04T19:54:59Z")),
                    Map.entry("pdf:hasCollection", List.of("false")),
                    Map.entry("pdf:encrypted", List.of("false")),
                    Map.entry("dc:title", List.of("facture")),
                    Map.entry("pdf:hasMarkedContent", List.of("false")),
                    Map.entry("pdf:ocrPageCount", List.of("0")),
                    Map.entry("Content-Type", List.of("application/pdf")),
                    Map.entry("access_permission:can_print_faithful", List.of("true")),
                    Map.entry("pdf:docinfo:creator", List.of("https://imagemagick.org")),
                    Map.entry("pdf:producer", List.of("https://imagemagick.org")),
                    Map.entry("access_permission:extract_for_accessibility", List.of("true")),
                    Map.entry("access_permission:assemble_document", List.of("true")),
                    Map.entry("xmpTPg:NPages", List.of("1")),
                    Map.entry("pdf:hasXMP", List.of("false")),
                    Map.entry("access_permission:extract_content", List.of("true")),
                    Map.entry("access_permission:can_print", List.of("true")),
                    Map.entry("X-TIKA:Parsed-By", List.of(
                            "org.apache.tika.parser.CompositeParser",
                            "org.apache.tika.parser.pdf.PDFParser"
                    )),
                    Map.entry("access_permission:can_modify", List.of("true")),
                    Map.entry("pdf:docinfo:producer", List.of("https://imagemagick.org")),
                    Map.entry("pdf:docinfo:created", List.of("2026-08-04T19:54:59Z"))
            ));
        } catch (final IOException | ImageMetadataExtractorException exception) {
            throw new RuntimeException(exception);
        }
    }
}
