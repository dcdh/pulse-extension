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
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@AsyncAggregateRootConsumerChannel(
        purpose = "statistics",
        sources = {
                @Source(applicationNaming = "TodoTaking"),
                @Source(applicationNaming = "TodoRegistered")})
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
        Log.info("StatisticsAggregateRootHandler.handleMessage() called from application: %s with purpose: %s".formatted(fromApplication.name(), purpose));
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
