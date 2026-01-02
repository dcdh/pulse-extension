package com.damdamdeo.pulse.extension.consumer.deployment.aggregateroot;

import com.damdamdeo.pulse.extension.consumer.runtime.Source;
import com.damdamdeo.pulse.extension.consumer.runtime.aggregateroot.AsyncAggregateRootConsumerChannel;
import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.DecryptablePayload;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import com.damdamdeo.pulse.extension.core.consumer.Purpose;
import com.damdamdeo.pulse.extension.core.consumer.aggregateroot.AsyncAggregateRootChannelMessageHandler;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@AsyncAggregateRootConsumerChannel(
        purpose = "statistics",
        sources = {
                @Source(functionalDomain = "TodoTaking", componentName = "Todo"),
                @Source(functionalDomain = "TodoClient", componentName = "Registered")})
public class StatisticsAggregateRootHandler implements AsyncAggregateRootChannelMessageHandler<JsonNode> {

    private Call call = null;

    @Override
    public void handleMessage(final FromApplication fromApplication,
                              final Purpose purpose,
                              final AggregateRootType aggregateRootType,
                              final AggregateId aggregateId,
                              final CurrentVersionInConsumption currentVersionInConsumption,
                              final EncryptedPayload encryptedPayload,
                              final OwnedBy ownedBy,
                              final BelongsTo belongsTo,
                              final DecryptablePayload<JsonNode> decryptablePayload) {
        this.call = new Call(fromApplication, purpose, aggregateRootType, aggregateId, currentVersionInConsumption,
                encryptedPayload, ownedBy, belongsTo, decryptablePayload);
    }

    public Call getCall() {
        return call;
    }

    public void reset() {
        call = null;
    }
}
