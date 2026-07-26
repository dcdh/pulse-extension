package com.damdamdeo.pulse.extension.query.runtime.gdpr.encryptable.deserializer;

import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.query.runtime.PulseQueryConfig;
import com.damdamdeo.pulse.extension.query.runtime.gdpr.encryptable.Encryptable;
import com.damdamdeo.pulse.extension.query.runtime.gdpr.encryptable.serializer.EncryptablePropertyWriter;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public final class EncryptableDeserializerModifier extends BeanDeserializerModifier {

    private final PulseQueryConfig pulseQueryConfig;
    private final DecryptionService decryptionService;

    public EncryptableDeserializerModifier(final PulseQueryConfig pulseQueryConfig,
                                           final DecryptionService decryptionService) {
        this.pulseQueryConfig = Objects.requireNonNull(pulseQueryConfig);
        this.decryptionService = Objects.requireNonNull(decryptionService);
    }

    @Override
    public BeanDeserializerBuilder updateBuilder(final DeserializationConfig config,
                                                 final BeanDescription beanDesc,
                                                 final BeanDeserializerBuilder builder) {
        final Iterator<SettableBeanProperty> it = builder.getProperties();
        final List<SettableBeanProperty> properties = new ArrayList<>();
        while (it.hasNext()) {
            properties.add(it.next());
        }
        for (final SettableBeanProperty property : properties) {
            if (Encryptable.class.isAssignableFrom(property.getType().getRawClass())) {
                SettableBeanProperty modifiedProp = property
                        .withName(new PropertyName(property.getName() + EncryptablePropertyWriter.ENCRYPTED_FIELD_SUFFIX))
                        .withValueDeserializer(new StdDeserializer<String>(String.class) {

                            @Override
                            public String deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException, JacksonException {
                                return "BOOM";
                            }
                        });

                builder.removeProperty(property.getFullName());
                builder.addProperty(modifiedProp);
            }
        }
        return builder;
    }
//    @Override
//    public BeanDeserializerBuilder updateBuilder(final DeserializationConfig config,
//                                                 final BeanDescription beanDesc,
//                                                 final BeanDeserializerBuilder builder) {
//        final Iterator<SettableBeanProperty> it = builder.getProperties();
//        final List<SettableBeanProperty> properties = new ArrayList<>();
//        while (it.hasNext()) {
//            properties.add(it.next());
//        }
//        for (final SettableBeanProperty property : properties) {
//            final AnnotatedMember member = property.getMember();
//            if (Encryptable.class.isAssignableFrom(member.getRawType())) {
//                Validate.validState(!property.getName().endsWith(EncryptablePropertyWriter.ENCRYPTED_FIELD_SUFFIX));
//                // property is <nom>_encrypted
//                final SettableBeanProperty encryptedProperty = property.withSimpleName(property.getName() + EncryptablePropertyWriter.ENCRYPTED_FIELD_SUFFIX);
//                final SettableBeanProperty reroutedProperty = new SettableBeanProperty.Delegating(encryptedProperty) {
//                    @Override
//                    protected SettableBeanProperty withDelegate(SettableBeanProperty newDelegate) {
//                        return this;
//                    }
//
//                    @Override
//                    public void deserializeAndSet(final JsonParser p, final DeserializationContext ctxt, final Object instance) throws IOException {
//                        final String encrypted = p.getValueAsString();
//                        if (encrypted != null) {
//                            try {
//                                final MasterKey masterKey = new MasterKey(pulseQueryConfig.masterKey());
//                                final EncryptedPayload encryptedPayload = new EncryptedPayload(encrypted.getBytes());
//                                final DecryptedPayload decryptedPayload = decryptionService.decrypt(encryptedPayload, masterKey.toPassphrase());
//                                property.set(instance, decryptedPayload.payload());
//                            } catch (DecryptionException e) {
//                                throw new RuntimeException(e);
//                            }
//                        } else {
//                            property.set(instance, null);
//                        }
//                    }
//                };
//                builder.addOrReplaceProperty(reroutedProperty, true);
//                builder.removeProperty(encryptedProperty.getFullName());
//            }
//        }
//        return builder;
//    }
//    @Override
//    public BeanDeserializerBuilder updateBuilder(final DeserializationConfig config,
//                                                 final BeanDescription beanDesc,
//                                                 final BeanDeserializerBuilder builder) {
//        final Iterator<SettableBeanProperty> it = builder.getProperties();
//        while (it.hasNext()) {
//            final SettableBeanProperty property = it.next();
//            if (Encryptable.class.isAssignableFrom(property.getType().getRawClass())) {
////                final JsonDeserializer<?> deser = new EncryptableDeserializer<>(property.getType(),
////                        pulseQueryConfig, decryptionService);
////                builder.addOrReplaceProperty(property.withValueDeserializer(deser), true);
//                final JsonDeserializer<?> deser =
//                        new EncryptableDeserializer<>(
//                                property.getType(),
//                                pulseQueryConfig,
//                                decryptionService
//                        );
//
//                final SettableBeanProperty newProperty = property.withValueDeserializer(deser);
//
//                builder.addOrReplaceProperty(newProperty, true);
//
//                // Important pour les records
//                builder.addCreatorProperty(newProperty);
//            }
//        }
//        return builder;
//    }
}
