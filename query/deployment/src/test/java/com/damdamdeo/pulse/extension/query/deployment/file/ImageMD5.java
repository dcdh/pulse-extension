package com.damdamdeo.pulse.extension.query.deployment.file;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.apache.commons.codec.digest.DigestUtils.md5;

public final class ImageMD5 {

    public static byte[] computeWithoutMetadata(final InputStream input) throws IOException {
        final BufferedImage image = ImageIO.read(input);
        if (image == null) {
            throw new IOException("Unable to read image");
        }

        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", output);

        return md5(new ByteArrayInputStream(output.toByteArray()));
    }
}
