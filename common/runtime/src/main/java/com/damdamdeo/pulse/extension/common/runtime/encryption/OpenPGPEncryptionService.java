package com.damdamdeo.pulse.extension.common.runtime.encryption;

import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.jcajce.JcePBEKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;
import java.util.Objects;

public class OpenPGPEncryptionService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    // Comply with 'pgp_sym_encrypt' used in Postgres
    public static EncryptedPayload encrypt(final byte[] clearData, final Passphrase passphrase) {
        Objects.requireNonNull(clearData);
        Objects.requireNonNull(passphrase);
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(
                    new JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5)
                            .setWithIntegrityPacket(true)
                            .setSecureRandom(new SecureRandom())
                            .setProvider("BC"));
            encGen.addMethod(new JcePBEKeyEncryptionMethodGenerator(passphrase.passphrase()).setProvider("BC"));

            try (final OutputStream encOut = encGen.open(out, new byte[1 << 16])) {
                final PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();
                try (OutputStream literalOut = lData.open(encOut, PGPLiteralData.BINARY, "data", clearData.length, new Date())) {
                    literalOut.write(clearData);
                }
            }
            encGen.close();
            return new EncryptedPayload(out.toByteArray());
        } catch (IOException | PGPException e) {
            throw new RuntimeException(e);
        }
    }
}
