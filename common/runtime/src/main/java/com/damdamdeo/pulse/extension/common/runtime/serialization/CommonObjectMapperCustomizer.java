package com.damdamdeo.pulse.extension.common.runtime.serialization;

import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.StoredAt;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

@Singleton
public final class CommonObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(final ObjectMapper objectMapper) {
        final SimpleModule module = new SimpleModule(CommonObjectMapperCustomizer.class.getName()) {
            @Override
            public void setupModule(final SetupContext context) {
                super.setupModule(context);
                context.addDeserializers(new Deserializers.Base() {
                    @Override
                    public JsonDeserializer<?> findBeanDeserializer(final JavaType type,
                                                                    final DeserializationConfig config,
                                                                    final BeanDescription beanDescription) {
                        if (type.hasRawClass(Encrypted.class)
                                && type.containedTypeCount() == 1
                                && type.containedType(0).hasRawClass(byte[].class)) {
                            return new EncryptedByteArrayDeserializer();
                        }
                        return null;
                    }
                });
            }
        };
        module.addSerializer(StoredAt.class, new StoredAtSerializer());
        module.addDeserializer(StoredAt.class, new StoredAtDeserializer());
        module.addSerializer(EventType.class, new EventTypeSerializer());
        module.addDeserializer(EventType.class, new EventTypeDeserializer());
        module.addSerializer(ExecutedByEncoded.class, new ExecutedByEncodedSerializer());
        module.addDeserializer(ExecutedByEncoded.class, new ExecutedByEncodedDeserializer());
        module.addSerializer(Encrypted.ByteArrayEncrypted.class, new EncryptedByteArraySerializer());
        objectMapper.registerModule(module);
    }
}
