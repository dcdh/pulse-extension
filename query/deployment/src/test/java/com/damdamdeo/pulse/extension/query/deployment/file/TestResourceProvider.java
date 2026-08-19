package com.damdamdeo.pulse.extension.query.deployment.file;

import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

public final class TestResourceProvider {

    private TestResourceProvider() {
    }

    public static Resource getResourceFromStream(final String resourceName) {
        Objects.requireNonNull(resourceName);
        Validate.validState(resourceName.startsWith("/"), "Resource name must start with /");
        final URL resource = Objects.requireNonNull(TestResourceProvider.class.getResource(resourceName));
        try {
            final long size = resource.openConnection().getContentLengthLong();
            final InputStream content = resource.openStream();
            return new Resource(content, size);
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
