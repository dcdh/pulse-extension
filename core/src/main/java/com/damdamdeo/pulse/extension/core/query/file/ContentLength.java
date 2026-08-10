package com.damdamdeo.pulse.extension.core.query.file;

import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record ContentLength(Long contentLength) {

    public static final ContentLength MAX = ContentLength.ofMegaBytes(5);

    public ContentLength {
        Objects.requireNonNull(contentLength);
        Validate.isTrue(contentLength > 0, "Content length must be greater than 0");
    }

    public static ContentLength ofMegaBytes(long megaBytes) {
        return new ContentLength(Math.multiplyExact(megaBytes, 1024L * 1024));
    }

    public void checkValid() throws MaxFileSizeReachedException {
        if (contentLength > MAX.contentLength) {
            throw new MaxFileSizeReachedException();
        }
    }
}
