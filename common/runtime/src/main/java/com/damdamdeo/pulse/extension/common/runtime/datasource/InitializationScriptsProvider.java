package com.damdamdeo.pulse.extension.common.runtime.datasource;

import java.util.List;
import java.util.Objects;

public final class InitializationScriptsProvider {

    private final List<PostgresSqlScript> postgresSqlScripts;

    public InitializationScriptsProvider(final List<PostgresSqlScript> postgresSqlScripts) {
        this.postgresSqlScripts = Objects.requireNonNull(postgresSqlScripts);
    }

    public List<PostgresSqlScript> provide() {
        return postgresSqlScripts;
    }
}
