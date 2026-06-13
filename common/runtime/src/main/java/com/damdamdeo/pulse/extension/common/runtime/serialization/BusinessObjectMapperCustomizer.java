package com.damdamdeo.pulse.extension.common.runtime.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface BusinessObjectMapperCustomizer {

    void customize(ObjectMapper objectMapper);
}
