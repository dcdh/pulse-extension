package com.damdamdeo.pulse.extension.common.runtime.datasource;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

import java.util.List;

@Recorder
public class PostgresSqlScriptsRecorder {

    public RuntimeValue<InitializationScriptsProvider> provide(final List<PostgresSqlScript> postgresSqlScripts) {
        return new RuntimeValue<>(new InitializationScriptsProvider(postgresSqlScripts));
    }
}
