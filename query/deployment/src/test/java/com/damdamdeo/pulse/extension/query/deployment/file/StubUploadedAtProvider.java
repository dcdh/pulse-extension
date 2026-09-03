package com.damdamdeo.pulse.extension.query.deployment.file;

import com.damdamdeo.pulse.extension.core.query.file.UploadedAt;
import com.damdamdeo.pulse.extension.core.query.file.UploadedAtProvider;
import com.damdamdeo.pulse.extension.core.query.file.query.UploadQueryTest;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

@ApplicationScoped
@Priority(1)
@Alternative
public class StubUploadedAtProvider implements UploadedAtProvider {

    @Override
    public UploadedAt now() {
        return UploadQueryTest.uploadedAt();
    }
}
