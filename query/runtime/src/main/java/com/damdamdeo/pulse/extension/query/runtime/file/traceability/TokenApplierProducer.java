package com.damdamdeo.pulse.extension.query.runtime.file.traceability;

import com.damdamdeo.pulse.extension.core.executedby.UsernameEncoder;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.file.traceability.*;
import io.quarkus.arc.All;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Produces;

import java.util.List;

@ApplicationScoped
public class TokenApplierProducer {

    @Produces
    @ApplicationScoped
    public TokenApplier tokenApplier(final TokenGenerator tokenGenerator,
                                     final TokenRepository tokenRepository,
                                     final ExecutionContextProvider executionContextProvider,
                                     final DownloadedAtProvider downloadedAtProvider,
                                     final UsernameEncoder usernameEncoder,
                                     @All final List<ContentTypeTokenApplier> contentTypeTokenAppliers) {
        return new TokenApplier(tokenGenerator, tokenRepository, executionContextProvider, downloadedAtProvider,
                usernameEncoder, contentTypeTokenAppliers);
    }
}
