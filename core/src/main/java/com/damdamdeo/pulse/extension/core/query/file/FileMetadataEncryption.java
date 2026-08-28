package com.damdamdeo.pulse.extension.core.query.file;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

public interface FileMetadataEncryption {

    EncryptedFileMetadata encrypt(FileMetadata fileMetadata, OwnedBy ownedBy) throws MetadataEncryptionException;

    FileMetadata decrypt(EncryptedFileMetadata encryptedFileMetadata) throws MetadataEncryptionException;
}
