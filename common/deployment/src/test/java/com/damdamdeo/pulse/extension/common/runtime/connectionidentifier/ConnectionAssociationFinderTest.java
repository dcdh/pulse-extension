package com.damdamdeo.pulse.extension.common.runtime.connectionidentifier;

import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionAssociationFinder;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectionAssociationFinderTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "true")
            .overrideRuntimeConfigKey("pulse.datasource.init-at-startup", "false")
            .withConfigurationResource("application.properties");

    @Inject
    Instance<ConnectionAssociationFinder> connectionAssociationFinderInstance;

    @Test
    void shouldRegisterConnectionIdentifierBean() {
        assertThat(connectionAssociationFinderInstance.isResolvable()).isTrue();
    }
}
