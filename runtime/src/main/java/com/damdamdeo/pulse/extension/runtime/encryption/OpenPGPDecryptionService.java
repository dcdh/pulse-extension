package com.damdamdeo.pulse.extension.runtime.encryption;

import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.PBEDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBEDataDecryptorFactoryBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.Objects;

@ApplicationScoped
@Unremovable
@DefaultBean
public final class OpenPGPDecryptionService implements DecryptionService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final PassphraseRepository passphraseRepository;

    public OpenPGPDecryptionService(final PassphraseRepository passphraseRepository) {
        this.passphraseRepository = Objects.requireNonNull(passphraseRepository);
    }

    @Override
    public DecryptedPayload decrypt(final EncryptedPayload encrypted, final OwnedBy ownedBy) throws DecryptionException {
        Objects.requireNonNull(encrypted);
        Objects.requireNonNull(ownedBy);
        try (final InputStream in = new ByteArrayInputStream(encrypted.payload())) {
            final PGPObjectFactory pgpF = new PGPObjectFactory(in, new JcaKeyFingerprintCalculator());
            final Object o = pgpF.nextObject();

            if (o instanceof PGPEncryptedDataList encList) {
                final PGPPBEEncryptedData encData = (PGPPBEEncryptedData) encList.get(0);

                final PBEDataDecryptorFactory decryptorFactory = new JcePBEDataDecryptorFactoryBuilder(
                        new JcaPGPDigestCalculatorProviderBuilder().build())
                        .setProvider("BC")
                        .build(passphraseRepository.retrieve(ownedBy)
                                .map(Passphrase::passphrase)
                                .orElseThrow(() -> new DecryptionException(ownedBy, "Unknown passphrase")));

                try (final InputStream clear = encData.getDataStream(decryptorFactory)) {
                    final PGPObjectFactory plainFact = new PGPObjectFactory(clear, new JcaKeyFingerprintCalculator());
                    final Object message = plainFact.nextObject();

                    if (message instanceof PGPLiteralData literalData) {
                        return new DecryptedPayload(literalData.getInputStream().readAllBytes());
                    } else {
                        throw new DecryptionException(ownedBy, "Contenu PGP inattendu, pas de PGPLiteralData");
                    }
                }
            }
            throw new DecryptionException(ownedBy, "Invalid PGP structure");
        } catch (final IOException | PGPException e) {
            throw new DecryptionException(ownedBy, e);
        }
    }
}
