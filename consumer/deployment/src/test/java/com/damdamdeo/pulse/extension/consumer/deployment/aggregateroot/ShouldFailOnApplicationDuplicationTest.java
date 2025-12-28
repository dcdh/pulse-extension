package com.damdamdeo.pulse.extension.consumer.deployment.aggregateroot;

import com.damdamdeo.pulse.extension.consumer.runtime.Source;
import com.damdamdeo.pulse.extension.consumer.runtime.aggregateroot.AsyncAggregateRootConsumerChannel;
import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.DecryptablePayload;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.Target;
import com.damdamdeo.pulse.extension.core.consumer.aggregateroot.AsyncAggregateRootChannelMessageHandler;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ShouldFailOnApplicationDuplicationTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.compose.devservices.enabled", "true")
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .withConfigurationResource("application.properties")
            .assertException(throwable -> assertThat(throwable)
                    .hasNoSuppressedExceptions()
                    .rootCause()
                    .isExactlyInstanceOf(IllegalArgumentException.class)
                    .hasMessage("functionalDomain 'TodoTaking' componentName 'Todo' declared more than once '2' in target 'statistics'")
                    .hasNoSuppressedExceptions());

    @Test
    void test() {
        Assertions.fail("Startup should have failed");
    }

    @ApplicationScoped
    @AsyncAggregateRootConsumerChannel(target = "statistics",
            sources = {
                    @Source(functionalDomain = "TodoTaking", componentName = "Todo"),
                    @Source(functionalDomain = "TodoTaking", componentName = "Todo")
            })
    static final class StatisticsEventHandler implements AsyncAggregateRootChannelMessageHandler<JsonNode> {

        @Override
        public void handleMessage(final FromApplication fromApplication,
                                  final Target target,
                                  final AggregateRootType aggregateRootType,
                                  final AggregateId aggregateId,
                                  final CurrentVersionInConsumption currentVersionInConsumption,
                                  final EncryptedPayload encryptedPayload,
                                  final OwnedBy ownedBy,
                                  final BelongsTo belongsTo,
                                  final DecryptablePayload<JsonNode> decryptablePayload) {
        }
    }
}
