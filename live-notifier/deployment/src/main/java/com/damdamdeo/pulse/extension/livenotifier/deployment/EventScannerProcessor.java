package com.damdamdeo.pulse.extension.livenotifier.deployment;

import com.damdamdeo.pulse.extension.livenotifier.deployment.items.EventBuildItem;
import com.damdamdeo.pulse.extension.livenotifier.runtime.LiveNotifierPublisher;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.jandex.*;

import java.util.List;
import java.util.Objects;

public class EventScannerProcessor {

    private static final DotName INJECT =
            DotName.createSimple("jakarta.inject.Inject");

    private static final DotName LIVE_NOTIFIER =
            DotName.createSimple(LiveNotifierPublisher.class.getName());

    @BuildStep
    List<EventBuildItem> discoverInjectedEventTypes(final CombinedIndexBuildItem indexBuildItem) {
        final IndexView index = indexBuildItem.getIndex();
        return index.getAnnotations(INJECT).stream()
                .map(AnnotationInstance::target)
                .filter(target -> target.kind() == AnnotationTarget.Kind.FIELD)
                .map(AnnotationTarget::asField)
                .map(FieldInfo::type)
                .filter(this::isLiveNotifierPublisher)
                .map(this::extractGenericType)
                .filter(Objects::nonNull)
                .distinct()
                .map(EventBuildItem::new)
                .toList();
    }

    private boolean isLiveNotifierPublisher(Type type) {
        return type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && type.name().equals(LIVE_NOTIFIER);
    }

    private Class<?> extractGenericType(final Type type) {
        final ParameterizedType pt = type.asParameterizedType();

        if (pt.arguments().isEmpty()) {
            return null;
        }

        final Type eventType = pt.arguments().getFirst();
        if (eventType.kind() != Type.Kind.CLASS) {
            return null;
        }

        try {
            return Thread.currentThread().getContextClassLoader().loadClass(eventType.name().toString());
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
