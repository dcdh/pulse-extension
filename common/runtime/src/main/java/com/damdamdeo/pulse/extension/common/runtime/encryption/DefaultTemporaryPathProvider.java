package com.damdamdeo.pulse.extension.common.runtime.encryption;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
@Unremovable
@DefaultBean
public class DefaultTemporaryPathProvider implements TemporaryPathProvider {

    @Override
    public Path provide() throws IOException {
        return Files.createTempFile("pulse-encrypted-", ".pgp");
    }
}
