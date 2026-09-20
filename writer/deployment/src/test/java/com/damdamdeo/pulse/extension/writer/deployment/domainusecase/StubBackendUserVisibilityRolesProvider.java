package com.damdamdeo.pulse.extension.writer.deployment.domainusecase;

import com.damdamdeo.pulse.extension.core.permission.BackendUserVisibilityRolesProvider;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.List;

@ApplicationScoped
@Priority(1)
@Alternative
public class StubBackendUserVisibilityRolesProvider implements BackendUserVisibilityRolesProvider {

    @Override
    public List<String> provide() {
        throw new IllegalStateException("Should not be called");
    }
}
