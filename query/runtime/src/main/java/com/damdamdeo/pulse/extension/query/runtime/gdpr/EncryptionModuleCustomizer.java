package com.damdamdeo.pulse.extension.query.runtime.gdpr;

import com.damdamdeo.pulse.extension.common.runtime.serialization.SequenceNumberDeserializer;
import com.damdamdeo.pulse.extension.common.runtime.serialization.SequenceNumberSerializer;
import com.damdamdeo.pulse.extension.core.SequenceNumber;
import com.damdamdeo.pulse.extension.core.encryption.DecryptionService;
import com.damdamdeo.pulse.extension.core.encryption.EncryptionService;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import com.damdamdeo.pulse.extension.query.runtime.PulseQueryConfig;
import com.damdamdeo.pulse.extension.query.runtime.gdpr.encryptable.deserializer.EncryptedAnnotationIntrospector;
import com.damdamdeo.pulse.extension.query.runtime.gdpr.encryptable.serializer.EncryptableSerializerModifier;
import com.damdamdeo.pulse.extension.query.runtime.gdpr.hashable.HashableSerializerModifier;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;

@ApplicationScoped
public class EncryptionModuleCustomizer implements ObjectMapperCustomizer {

    private final PulseQueryConfig pulseQueryConfig;
    private final EncryptionService encryptionService;
    private final DecryptionService decryptionService;
    private final Hasher hasher;

    public EncryptionModuleCustomizer(final Hasher hasher,
                                      final PulseQueryConfig pulseQueryConfig,
                                      final EncryptionService encryptionService,
                                      final DecryptionService decryptionService) {
        this.hasher = Objects.requireNonNull(hasher);
        this.pulseQueryConfig = Objects.requireNonNull(pulseQueryConfig);
        this.encryptionService = Objects.requireNonNull(encryptionService);
        this.decryptionService = Objects.requireNonNull(decryptionService);
    }

    @Override
    public void customize(final ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper);
        final SimpleModule hashableModule = new SimpleModule();
        hashableModule.setSerializerModifier(new HashableSerializerModifier(hasher));
        objectMapper.registerModule(hashableModule);
        final SimpleModule encryptableModule = new SimpleModule();
        encryptableModule.setSerializerModifier(new EncryptableSerializerModifier(pulseQueryConfig, encryptionService));
        objectMapper.registerModule(encryptableModule);
        final SimpleModule module = new SimpleModule();
        module.addSerializer(SequenceNumber.class, new SequenceNumberSerializer());
        module.addDeserializer(SequenceNumber.class, new SequenceNumberDeserializer());
        module.addSerializer(OwnedBy.class, new OwnedBySerializer());
        module.addDeserializer(OwnedBy.class, new OwnedByDeserializer());
        objectMapper.registerModule(module);
        objectMapper.setAnnotationIntrospector(AnnotationIntrospector.pair(
                new EncryptedAnnotationIntrospector(pulseQueryConfig, decryptionService),
                objectMapper.getDeserializationConfig().getAnnotationIntrospector()
        ));
    }
}
