package com.damdamdeo.pulse.extension.common.runtime.encryption;

import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.encryption.EncryptionException;
import com.damdamdeo.pulse.extension.core.encryption.EncryptionService;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
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

@ApplicationScoped
@Unremovable
@DefaultBean
public final class OpenPGPEncryptionService implements EncryptionService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    // Comply with 'pgp_sym_encrypt' used in Postgres
    public EncryptedPayload encrypt(final byte[] clearData, final Passphrase passphrase) throws EncryptionException {
        Objects.requireNonNull(clearData);
        Objects.requireNonNull(passphrase);
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(
                    new JcePGPDataEncryptorBuilder(PGPEncryptedData.AES_128)
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
        } catch (final IOException | PGPException e) {
            throw new EncryptionException(e);
        }
    }
}
