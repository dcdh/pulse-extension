package com.damdamdeo.pulse.extension.core.query.file;

import java.util.Arrays;
import java.util.Objects;

public enum ContentType {

    APPLICATION_PDF {
        @Override
        public String extension() {
            return "pdf";
        }

        @Override
        public String contentType() {
            return "application/pdf";
        }
    },

    IMAGE_JPEG {
        @Override
        public String extension() {
            return "jpeg";
        }

        @Override
        public String contentType() {
            return "image/jpeg";
        }
    },

    IMAGE_JPG {
        @Override
        public String extension() {
            return "jpg";
        }

        @Override
        public String contentType() {
            return "image/jpg";
        }
    },

    IMAGE_PNG {
        @Override
        public String extension() {
            return "png";
        }

        @Override
        public String contentType() {
            return "image/png";
        }
    };

    public abstract String extension();

    public abstract String contentType();

    public static ContentType fromContentType(final String contentType) {
        Objects.requireNonNull(contentType);
        return Arrays.stream(ContentType.values())
                .filter(value -> contentType.equals(value.contentType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unknown content type " + contentType));
    }
}
