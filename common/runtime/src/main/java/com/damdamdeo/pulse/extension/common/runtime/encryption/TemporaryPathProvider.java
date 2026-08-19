package com.damdamdeo.pulse.extension.common.runtime.encryption;

import java.io.IOException;
import java.nio.file.Path;

public interface TemporaryPathProvider {

    Path provide() throws IOException;
}
