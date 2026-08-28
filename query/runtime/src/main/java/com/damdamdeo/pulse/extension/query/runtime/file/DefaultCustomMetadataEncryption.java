package com.damdamdeo.pulse.extension.query.runtime.file;

import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.query.file.CustomMetadata;
import com.damdamdeo.pulse.extension.core.query.file.CustomMetadataEncryption;
import com.damdamdeo.pulse.extension.core.query.file.EncryptedCustomMetadata;
import com.damdamdeo.pulse.extension.core.query.file.MetadataEncryptionException;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
@DefaultBean
@Unremovable
public class DefaultCustomMetadataEncryption implements CustomMetadataEncryption {

    private final EncryptionService encryptionService;
    private final DecryptionService decryptionService;

    public DefaultCustomMetadataEncryption(final EncryptionService encryptionService,
                                           final DecryptionService decryptionService) {
        this.encryptionService = Objects.requireNonNull(encryptionService);
        this.decryptionService = Objects.requireNonNull(decryptionService);
    }

    @Override
    public EncryptedCustomMetadata encrypt(final CustomMetadata customMetadata, final OwnedBy ownedBy) throws MetadataEncryptionException {
        Objects.requireNonNull(customMetadata);
        Objects.requireNonNull(ownedBy);
        try {
            final byte[] packed = pack(customMetadata);
            return new EncryptedCustomMetadata(encryptionService.encrypt(
                    new ByteArrayInputStream(packed), ownedBy, t -> {
                        try (final InputStream payload = t.payload()) {
                            return Encrypted.of(payload.readAllBytes());
                        }
                    }), ownedBy);
        } catch (final EncryptionException | IOException exception) {
            throw new MetadataEncryptionException(exception);
        }
    }

    @Override
    public CustomMetadata decrypt(final EncryptedCustomMetadata encryptedCustomMetadata) throws MetadataEncryptionException {
        Objects.requireNonNull(encryptedCustomMetadata);
        try {
            final Decrypted<byte[]> decrypted = decryptionService.decrypt(encryptedCustomMetadata.encrypted(), encryptedCustomMetadata.ownedBy());
            return unpack(decrypted);
        } catch (final DecryptionException | IOException exception) {
            throw new MetadataEncryptionException(exception);
        }
    }

    private byte[] pack(final CustomMetadata customMetadata) throws IOException {
        try (final MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
            final Map<String, String> metadata = customMetadata.metadata();
            packer.packMapHeader(metadata.size());
            for (final Map.Entry<String, String> entry : metadata.entrySet()) {
                packer.packString(entry.getKey());
                packer.packString(entry.getValue());
            }
            return packer.toByteArray();
        }
    }

    private CustomMetadata unpack(Decrypted<byte[]> decrypted) throws IOException {
        try (final MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(decrypted.payload())) {
            final int mapSize = unpacker.unpackMapHeader();
            final Map<String, String> metadata = new HashMap<>(mapSize);
            for (int i = 0; i < mapSize; i++) {
                String key = unpacker.unpackString();
                String value = unpacker.unpackString();
                metadata.put(key, value);
            }
            return new CustomMetadata(metadata);
        }
    }
}
