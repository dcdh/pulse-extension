package com.damdamdeo.pulse.extension.common.runtime.hashing;

import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class HasherTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    @Inject
    Hasher hasher;

    record CustomIdentifiable(String id) implements Identifiable {

        CustomIdentifiable {
            Objects.requireNonNull(id);
        }
    }

    @Test
    void shouldHashUsingSHA3_256Test() {
        // Given
        final CustomIdentifiable givenOriginal = new CustomIdentifiable("test");

        // When
        final Hash<CustomIdentifiable> hash = hasher.hash(givenOriginal);

        // Then
        assertThat(hash).isEqualTo(new Hash<CustomIdentifiable>("36f028580bb02cc8272a9a020f4200e346e276ae664e45ee80745574e2f5ab80"));
    }
}
