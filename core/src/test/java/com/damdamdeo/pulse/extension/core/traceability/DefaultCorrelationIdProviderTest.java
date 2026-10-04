package com.damdamdeo.pulse.extension.core.traceability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DefaultCorrelationIdProviderTest {

    @Mock
    private CorrelationIdGenerator correlationIdGenerator;

    @Mock
    private CorrelationIdProviderContextualStorage correlationIdProviderContextualStorage;

    private DefaultCorrelationIdProvider correlationIdProvider;

    @BeforeEach
    void setUp() {
        correlationIdProvider = new DefaultCorrelationIdProvider(
                correlationIdGenerator,
                correlationIdProviderContextualStorage
        );
    }

    @Test
    void shouldProvideCorrelationIdFromContextualStorageWhenPresent() throws Exception {
        // given
        final CorrelationId storedCorrelationId = new CorrelationId(123L);
        given(correlationIdProviderContextualStorage.retrieve())
                .willReturn(Optional.of(storedCorrelationId));

        // when
        final CorrelationId correlationId = correlationIdProvider.provide();

        // then
        assertSame(storedCorrelationId, correlationId);
        then(correlationIdProviderContextualStorage).should().retrieve();
        then(correlationIdGenerator).shouldHaveNoInteractions();
        then(correlationIdProviderContextualStorage).should(never()).store(any());
    }

    @Test
    void shouldGenerateStoreAndProvideCorrelationIdWhenContextualStorageIsEmpty() throws Exception {
        // given
        final CorrelationId generatedCorrelationId = new CorrelationId(123L);
        final CorrelationId storedCorrelationId = new CorrelationId(456L);
        given(correlationIdProviderContextualStorage.retrieve()).willReturn(Optional.empty());
        given(correlationIdGenerator.generate()).willReturn(generatedCorrelationId);
        given(correlationIdProviderContextualStorage.store(generatedCorrelationId))
                .willReturn(storedCorrelationId);

        // when
        final CorrelationId correlationId = correlationIdProvider.provide();

        // then
        assertSame(storedCorrelationId, correlationId);
        final InOrder inOrder = inOrder(
                correlationIdProviderContextualStorage,
                correlationIdGenerator
        );
        inOrder.verify(correlationIdProviderContextualStorage).retrieve();
        inOrder.verify(correlationIdGenerator).generate();
        inOrder.verify(correlationIdProviderContextualStorage).store(generatedCorrelationId);
    }

    @Test
    void shouldWrapGeneratorExceptionAndNotStoreCorrelationId() throws Exception {
        // given
        final CorrelationIdGeneratorException generatorException =
                new CorrelationIdGeneratorException(new RuntimeException("generation failure"));
        given(correlationIdProviderContextualStorage.retrieve()).willReturn(Optional.empty());
        given(correlationIdGenerator.generate()).willThrow(generatorException);

        // when
        final CorrelationIdProviderException providerException = assertThrows(
                CorrelationIdProviderException.class,
                correlationIdProvider::provide
        );

        // then
        assertSame(generatorException, providerException.getCause());
        final InOrder inOrder = inOrder(
                correlationIdProviderContextualStorage,
                correlationIdGenerator
        );
        inOrder.verify(correlationIdProviderContextualStorage).retrieve();
        inOrder.verify(correlationIdGenerator).generate();
        then(correlationIdProviderContextualStorage).should(never()).store(any());
    }
}
