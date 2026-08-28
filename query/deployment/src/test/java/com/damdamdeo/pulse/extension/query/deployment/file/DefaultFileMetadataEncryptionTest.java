package com.damdamdeo.pulse.extension.query.deployment.file;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.query.file.EncryptedFileMetadata;
import com.damdamdeo.pulse.extension.core.query.file.FileMetadata;
import com.damdamdeo.pulse.extension.core.query.file.MetadataEncryptionException;
import com.damdamdeo.pulse.extension.query.deployment.StubPassphraseProvider;
import com.damdamdeo.pulse.extension.query.runtime.file.DefaultFileMetadataEncryption;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultFileMetadataEncryptionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(StubPassphraseProvider.class))
            .withConfigurationResource("application.properties");

    @Inject
    DefaultFileMetadataEncryption defaultFileMetadataEncryption;

    @Test
    void shouldEncryptAndDecrypt() throws MetadataEncryptionException {
        // Given
        final FileMetadata givenFileMetadata = new FileMetadata(Map.of("key", List.of("value")));

        // When
        final EncryptedFileMetadata encrypted = defaultFileMetadataEncryption.encrypt(givenFileMetadata, OwnedBy.from(TodoId.USER_1_TODO_1));
        final FileMetadata decrypted = defaultFileMetadataEncryption.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(givenFileMetadata);
    }
}
