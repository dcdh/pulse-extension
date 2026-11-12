package com.damdamdeo.pulse.extension.it.query.todo;

import com.damdamdeo.pulse.extension.common.runtime.serialization.PulseObjectMapperCustomizer;
import com.damdamdeo.pulse.extension.core.Description;
import com.damdamdeo.pulse.extension.core.Owner;
import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.TodoChecklistId;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import com.damdamdeo.pulse.extension.core.query.TodoChecklistProjection;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import com.damdamdeo.pulse.extension.query.runtime.PulseQueryConfig;
import com.damdamdeo.pulse.extension.query.runtime.serialization.ProjectionIdObjectMapperCustomizer;
import com.damdamdeo.pulse.extension.query.runtime.gdpr.EncryptionModuleCustomizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoObjectMapperCustomizerTest {

    private static final String MASTER_KEY = "0123456789abcdef0123456789abcdef";

    private static final Description DESCRIPTION = new Description("Description confidentielle");

    private static final byte[] ENCRYPTED = "encrypted-description".getBytes(StandardCharsets.UTF_8);

    // language=json
    private static final String JSON = """
            {
              "description_encrypted": "ZW5jcnlwdGVkLWRlc2NyaXB0aW9u"
            }
            """;

    @Mock
    PulseQueryConfig config;

    @Mock
    EncryptionService encryptionService;

    @Mock
    DecryptionService decryptionService;

    @Mock
    Hasher hasher;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        when(config.masterKey()).thenReturn(MASTER_KEY);
        objectMapper = TodoObjectMapperCustomizer.internal(new ObjectMapper());
        new PulseObjectMapperCustomizer().customize(objectMapper);
        new ProjectionIdObjectMapperCustomizer().customize(objectMapper);
        new EncryptionModuleCustomizer(hasher, config, encryptionService, decryptionService).customize(objectMapper);
    }

    @Test
    void shouldSerializeSensitiveDescription() throws Exception {
        // Given
        stubEncryption();

        // When
        final String json = objectMapper.writeValueAsString(DESCRIPTION);

        // Then
        assertAll(
                () -> assertEquals(objectMapper.readTree(JSON), objectMapper.readTree(json)),
                () -> assertFalse(json.contains(DESCRIPTION.description())),
                () -> verifyNoInteractions(decryptionService, hasher));
    }

    @Test
    void shouldDeserializeSensitiveDescription() throws Exception {
        // Given
        stubDecryption();

        // When
        final Description description = objectMapper.readValue(JSON, Description.class);

        // Then
        assertAll(
                () -> assertEquals(DESCRIPTION, description),
                () -> verify(decryptionService).decrypt(eq(Encrypted.of(ENCRYPTED)), eq(passphrase())),
                () -> verifyNoInteractions(encryptionService, hasher));
    }

    @Test
    void shouldRoundTripSensitiveDescriptionThroughJson() throws Exception {
        // Given
        stubEncryption();
        stubDecryption();

        // When
        final String json = objectMapper.writeValueAsString(DESCRIPTION);
        final Description description = objectMapper.readValue(json, Description.class);

        // Then
        assertAll(
                () -> assertEquals(DESCRIPTION, description),
                () -> assertEquals(objectMapper.readTree(JSON), objectMapper.readTree(json)),
                () -> verifyNoInteractions(hasher));
    }

    @Test
    void shouldRoundTripProjectionThroughJson() throws Exception {
        // Given
        stubEncryption();
        stubDecryption();
        final TodoProjection projection = new TodoProjection(ProjectionId.from(TodoId.USER_1_TODO_1),
                DESCRIPTION, Status.IN_PROGRESS, true,
                List.of(new TodoChecklistProjection(ProjectionId.from(TodoChecklistId.USER_1_TODO_1_1), "Checklist")),
                new Owner(UserId.USER_2));

        // When
        final String json = objectMapper.writeValueAsString(projection);
        final TodoProjection restored = objectMapper.readValue(json,
                TodoProjection.class);

        // Then
        assertAll(
                () -> assertEquals(projection, restored),
                () -> assertEquals(objectMapper.readTree(JSON), objectMapper.readTree(json).get("description")),
                () -> assertFalse(json.contains(DESCRIPTION.description())),
                () -> verifyNoInteractions(hasher));
    }

    private void stubEncryption() throws Exception {
        when(encryptionService.encrypt(any(InputStream.class), eq(passphrase()), any()))
                .thenAnswer(invocation -> {
                    final InputStream clearData = invocation.getArgument(0);
                    assertEquals(DESCRIPTION.description(), new String(clearData.readAllBytes(), StandardCharsets.UTF_8));
                    return Encrypted.of(ENCRYPTED);
                });
    }

    private void stubDecryption() throws Exception {
        when(decryptionService.decrypt(eq(Encrypted.of(ENCRYPTED)), eq(passphrase())))
                .thenReturn(new Decrypted<>(DESCRIPTION.description().getBytes(StandardCharsets.UTF_8)));
    }

    private static Passphrase passphrase() {
        return new MasterKey(MASTER_KEY).toPassphrase();
    }
}
