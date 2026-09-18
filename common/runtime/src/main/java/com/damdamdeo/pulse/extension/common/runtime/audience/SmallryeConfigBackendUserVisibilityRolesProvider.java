package com.damdamdeo.pulse.extension.common.runtime.audience;

import com.damdamdeo.pulse.extension.common.runtime.BackendUserConfiguration;
import com.damdamdeo.pulse.extension.core.audience.BackendUserVisibilityRolesProvider;
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
    BackendUserConfiguration backendUserConfiguration;

    @Override
    public List<String> provide() {
        return backendUserConfiguration.visibility().roles().orElseGet(List::of);
    }
}
