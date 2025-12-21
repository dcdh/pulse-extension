package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.common.deployment.items.EligibleTypeForSerializationBuildItem;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.event.Event;
import io.quarkus.deployment.annotations.BuildStep;

import java.util.List;

public class WriterSerializerProcessor {

    @BuildStep
    public List<EligibleTypeForSerializationBuildItem> eligibleTypeForSerializationBuildItemsToSerialize() {
        return List.of(new EligibleTypeForSerializationBuildItem(AggregateRoot.class),
                new EligibleTypeForSerializationBuildItem(Event.class));
    }
}
