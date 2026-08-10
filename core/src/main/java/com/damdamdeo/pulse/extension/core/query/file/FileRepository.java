package com.damdamdeo.pulse.extension.core.query.file;

import com.damdamdeo.pulse.extension.core.encryption.Encrypted;

import java.io.InputStream;

public interface FileRepository {

    boolean exists(FileIdentifier fileIdentifier) throws FileRepositoryException;

    FileInfo store(FileInfo fileInfo, Encrypted<InputStream> encrypted) throws FileRepositoryException;

    FileInfo getFileInfoByFileIdentifier(FileIdentifier fileIdentifier) throws FileRepositoryException;

    FileContent getFileContentByFileIdentifier(FileIdentifier fileIdentifier) throws FileRepositoryException;
}
