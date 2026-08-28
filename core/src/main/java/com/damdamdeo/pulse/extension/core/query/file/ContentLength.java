package com.damdamdeo.pulse.extension.core.query.file;

import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record ContentLength(Long contentLength) {

    public static final ContentLength MAX = ContentLength.ofMegaBytes(5L);

    public ContentLength {
        Objects.requireNonNull(contentLength);
        Validate.isTrue(contentLength > 0, "Content length must be greater than 0");
    }

    public static ContentLength ofMegaBytes(Long megaBytes) {
        Objects.requireNonNull(megaBytes);
        Validate.validState(megaBytes > 0, "Mega bytes must be greater than 0");
        return new ContentLength(Math.multiplyExact(megaBytes, 1024 * 1024));
    }

    public void checkValid() throws MaxFileSizeReachedException {
        if (contentLength > MAX.contentLength) {
            throw new MaxFileSizeReachedException();
        }
    }
}
