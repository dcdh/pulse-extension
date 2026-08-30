package com.damdamdeo.pulse.extension.traceability.deployment.finder;

import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.connecteduser.UsernameEncoded;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.UnableToDecodeException;
import com.damdamdeo.pulse.extension.core.executedby.UsernameDecoder;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.Objects;

@ApplicationScoped
@Priority(1)
@Alternative
public class StubUsernameDecoder implements UsernameDecoder {

    @Override
    public Username decode(final UsernameEncoded usernameEncoded, final OwnedBy ownedBy) throws UnableToDecodeException {
        Objects.requireNonNull(usernameEncoded);
        Objects.requireNonNull(ownedBy);
        if (usernameEncoded.encoded().equals("aliceEncoded")) {
            return new Username("alice@mail.com");
        } else if (usernameEncoded.encoded().equals("bobEncoded")) {
            return new Username("bob@mail.com");
        } else {
            throw new IllegalStateException("Should not be here");
        }
    }
}
