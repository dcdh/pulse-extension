package com.damdamdeo.pulse.extension.query.runtime.file;

import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.query.file.EncryptedFileMetadata;
import com.damdamdeo.pulse.extension.core.query.file.FileMetadata;
import com.damdamdeo.pulse.extension.core.query.file.FileMetadataEncryption;
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
import java.util.*;

@ApplicationScoped
@DefaultBean
@Unremovable
public class DefaultFileMetadataEncryption implements FileMetadataEncryption {

    private final EncryptionService encryptionService;
    private final DecryptionService decryptionService;

    public DefaultFileMetadataEncryption(final EncryptionService encryptionService,
                                         final DecryptionService decryptionService) {
        this.encryptionService = Objects.requireNonNull(encryptionService);
        this.decryptionService = Objects.requireNonNull(decryptionService);
    }

    @Override
    public EncryptedFileMetadata encrypt(final FileMetadata fileMetadata, final OwnedBy ownedBy) throws MetadataEncryptionException {
        Objects.requireNonNull(fileMetadata);
        Objects.requireNonNull(ownedBy);
        try {
            final byte[] packed = pack(fileMetadata);
            return new EncryptedFileMetadata(encryptionService.encrypt(
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
    public FileMetadata decrypt(final EncryptedFileMetadata encryptedFileMetadata) throws MetadataEncryptionException {
        Objects.requireNonNull(encryptedFileMetadata);
        try {
            final Decrypted<byte[]> decrypted = decryptionService.decrypt(encryptedFileMetadata.encrypted(), encryptedFileMetadata.ownedBy());
            return unpack(decrypted);
        } catch (final DecryptionException | IOException exception) {
            throw new MetadataEncryptionException(exception);
        }
    }

    private byte[] pack(FileMetadata fileMetadata) throws IOException {
        try (final MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
            final Map<String, List<String>> metadata = fileMetadata.metadata();
            packer.packMapHeader(metadata.size());
            for (final Map.Entry<String, List<String>> entry : metadata.entrySet()) {
                packer.packString(entry.getKey());
                final List<String> values = entry.getValue();
                packer.packArrayHeader(values.size());
                for (final String value : values) {
                    packer.packString(value);
                }
            }
            return packer.toByteArray();
        }
    }

    private FileMetadata unpack(Decrypted<byte[]> decrypted) throws IOException {
        try (final MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(decrypted.payload())) {
            final int mapSize = unpacker.unpackMapHeader();
            final Map<String, List<String>> metadata = new HashMap<>(mapSize);
            for (int i = 0; i < mapSize; i++) {
                final String key = unpacker.unpackString();
                final int arraySize = unpacker.unpackArrayHeader();
                final List<String> values = new ArrayList<>(arraySize);
                for (int j = 0; j < arraySize; j++) {
                    values.add(unpacker.unpackString());
                }
                metadata.put(key, Collections.unmodifiableList(values));
            }
            return new FileMetadata(metadata);
        }
    }
}
