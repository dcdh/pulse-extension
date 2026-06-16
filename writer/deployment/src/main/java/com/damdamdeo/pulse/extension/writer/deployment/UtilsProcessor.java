package com.damdamdeo.pulse.extension.writer.deployment;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import java.util.Objects;

public final class UtilsProcessor {

    private UtilsProcessor() {
    }

    public static boolean hasDirectImplementation(final ClassInfo classInfo, final IndexView index, final Class<?> clazz) {
        Objects.requireNonNull(classInfo);
        Objects.requireNonNull(index);
        Objects.requireNonNull(clazz);
        if (classInfo.interfaceNames().contains(DotName.createSimple(clazz))) {
            return true;
        }
        for (final DotName interfaceName : classInfo.interfaceNames()) {
            final ClassInfo interfaceInfo = index.getClassByName(interfaceName);
            if (interfaceInfo != null && hasDirectImplementation(interfaceInfo, index, clazz)) {
                return true;
            }
        }
        return false;
    }
}
