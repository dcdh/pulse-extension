package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.executedby.UsernameHasher;

import java.util.Objects;

public final class DefaultTraceAppender implements TraceAppender {

    private final ExecutionContextProvider executionContextProvider;
    private final ExecutedAtProvider executedAtProvider;
    private final TraceIdGenerator traceIdGenerator;
    private final UsernameHasher usernameHasher;
    private final TraceRepository traceRepository;

    public DefaultTraceAppender(final ExecutionContextProvider executionContextProvider,
                                final ExecutedAtProvider executedAtProvider,
                                final TraceIdGenerator traceIdGenerator,
                                final UsernameHasher usernameHasher,
                                final TraceRepository traceRepository) {
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.executedAtProvider = Objects.requireNonNull(executedAtProvider);
        this.traceIdGenerator = Objects.requireNonNull(traceIdGenerator);
        this.usernameHasher = Objects.requireNonNull(usernameHasher);
        this.traceRepository = Objects.requireNonNull(traceRepository);
    }

    @Override
    public void append(final Traceable traceable, final ExecutionStatus executionStatus) throws TraceAppenderException {
        Objects.requireNonNull(traceable);
        Objects.requireNonNull(executionStatus);
        try {
            FCK je ne peux pas utiliser le mecanisme d'ownership ... c'est compliqué si je retourne une liste
                    le executedBy devrait être lié à un compte commun ...
            je dois le faire au niveau de chaque trace ou bien
            FCK c'est la vision item qui me pose pb
                    
            traceRepository.store(new Trace(
                    traceIdGenerator.generate(),
                    TracedByHashed.from(executionContextProvider.provide().executedBy().hash(usernameHasher)),
                    ,
                    ,
                    executedAtProvider.now(),
                    traceable.executedOn(),
                    executionStatus));
        } catch (final TraceIdGeneratorException | TraceRepositoryException exception) {
            throw new TraceAppenderException(exception);
        }
    }
}

// FCK il me faut savoir si cela à reussi ou echouer c'est pour cela que je dois plutot passer par un interceptor ... cote infrastructure
// FCK comment faire dans le guard ... uniquemment si cela passe ou pas ?
