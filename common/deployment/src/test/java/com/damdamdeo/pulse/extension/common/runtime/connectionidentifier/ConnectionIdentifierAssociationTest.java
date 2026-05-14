package com.damdamdeo.pulse.extension.common.runtime.connectionidentifier;

import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierAssociation;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectionIdentifierAssociationTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "true")
            .overrideConfigKey("pulse.datasource.init-at-startup", "false")
            .withConfigurationResource("application.properties");

    @Inject
    Instance<ConnectionIdentifierAssociation> connectionIdentifierAssociationInstance;

    @Test
    void shouldRegisterConnectionIdentifierBean() {
        assertThat(connectionIdentifierAssociationInstance.isResolvable()).isTrue();
    }
}
