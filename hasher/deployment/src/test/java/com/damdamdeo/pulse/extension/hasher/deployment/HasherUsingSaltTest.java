package com.damdamdeo.pulse.extension.hasher.deployment;

import com.damdamdeo.pulse.extension.core.hashing.Hash;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import com.damdamdeo.pulse.extension.hasher.runtime.CustomIdentifiable;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class HasherUsingSaltTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .overrideRuntimeConfigKey("pulse.hasher.pepper", "customPepper")
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
                () -> assertThat(hash).isEqualTo(given.expectedWithPepper())
        );
    }

    @Test
    void shouldBeDeterministic() {
        // Given
        final CustomIdentifiable given = CustomIdentifiable.GIVEN;

        // When
        final List<Hash<CustomIdentifiable>> executed = new ArrayList<>(3);
        for (int run = 0; run < 3; run++) {
            executed.add(hasher.hash(given));
        }

        assertThat(executed).containsExactly(given.expectedWithPepper(), given.expectedWithPepper(), given.expectedWithPepper());
    }
}
