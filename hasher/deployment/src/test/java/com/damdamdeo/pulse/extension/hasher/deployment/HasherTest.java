package com.damdamdeo.pulse.extension.hasher.deployment;

import com.damdamdeo.pulse.extension.core.hashing.Hash;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import com.damdamdeo.pulse.extension.hasher.runtime.CustomIdentifiable;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class HasherTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    @Inject
    Hasher hasher;

    @Test
    void shouldHashUsingSHA3_256Test() {
        // Given
        final CustomIdentifiable given = CustomIdentifiable.GIVEN;

        // When
        final Hash<CustomIdentifiable> hash = hasher.hash(given);

        // Then
        assertAll(
                () -> assertThat(hasher.getClass().getName()).isEqualTo("com.damdamdeo.pulse.extension.hasher.runtime.Sha3256DefaultHasher_ClientProxy"),
                () -> assertThat(hash).isEqualTo(given.expected())
        );
    }
}
