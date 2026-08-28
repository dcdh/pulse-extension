package com.damdamdeo.pulse.extension.core.query.file;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

public interface CustomMetadataEncryption {

    EncryptedCustomMetadata encrypt(CustomMetadata customMetadata, OwnedBy ownedBy) throws MetadataEncryptionException;

    CustomMetadata decrypt(EncryptedCustomMetadata encryptedCustomMetadata) throws MetadataEncryptionException;
}
