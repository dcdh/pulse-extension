package com.damdamdeo.pulse.extension.query.runtime;

import com.damdamdeo.pulse.extension.core.query.BackendUserVisibilityRolesProvider;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
@Unremovable
@DefaultBean
public class SmallryeConfigBackendUserVisibilityRolesProvider implements BackendUserVisibilityRolesProvider {

    @Inject
    BackendUserVisibilityRolesConfig backendUserVisibilityRolesConfig;

    @Override
    public List<String> provide() {
        return backendUserVisibilityRolesConfig.roles();
    }
}
