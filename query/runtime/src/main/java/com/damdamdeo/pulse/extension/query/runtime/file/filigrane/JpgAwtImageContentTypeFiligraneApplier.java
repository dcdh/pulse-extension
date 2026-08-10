package com.damdamdeo.pulse.extension.query.runtime.file.filigrane;

import com.damdamdeo.pulse.extension.core.query.file.ContentType;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Unremovable
public class JpgAwtImageContentTypeFiligraneApplier extends AwtImageContentTypeFiligraneApplier {

    @Override
    public ContentType contentType() {
        return ContentType.IMAGE_JPG;
    }
}
