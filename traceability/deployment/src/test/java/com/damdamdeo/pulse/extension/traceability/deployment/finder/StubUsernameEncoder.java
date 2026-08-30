package com.damdamdeo.pulse.extension.traceability.deployment.finder;

import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.connecteduser.UsernameEncoded;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.UnableToEncodeException;
import com.damdamdeo.pulse.extension.core.executedby.UsernameEncoder;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.Objects;

@ApplicationScoped
@Priority(1)
@Alternative
public class StubUsernameEncoder implements UsernameEncoder {

    @Override
    public UsernameEncoded encode(final Username username, final OwnedBy ownedBy) throws UnableToEncodeException {
        Objects.requireNonNull(username);
        Objects.requireNonNull(ownedBy);
        if (new Username("alice@mail.com").equals(username)) {
            return new UsernameEncoded("aliceEncoded");
        } else if (new Username("bob@mail.com").equals(username)) {
            return new UsernameEncoded("bobEncoded");
        } else {
            throw new IllegalStateException("Should not be here");
        }
    }
}
