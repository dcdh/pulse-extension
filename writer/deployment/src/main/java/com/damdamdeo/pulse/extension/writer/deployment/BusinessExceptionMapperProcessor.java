package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.writer.runtime.BusinessExceptionMapper;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;

public class BusinessExceptionMapperProcessor {

    @BuildStep
    AdditionalIndexedClassesBuildItem additionalIndexedClasses() {
        return new AdditionalIndexedClassesBuildItem(BusinessExceptionMapper.class.getName());
    }
}
