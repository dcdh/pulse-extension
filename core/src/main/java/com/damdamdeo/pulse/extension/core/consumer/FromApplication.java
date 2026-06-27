package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.ApplicationNaming;

import java.util.Objects;

public final class FromApplication {
    private final ApplicationNaming applicationNaming;

    public FromApplication(final ApplicationNaming applicationNaming) {
        Objects.requireNonNull(applicationNaming);
        this.applicationNaming = applicationNaming;
    }

    public FromApplication(final String applicationNaming) {
        Objects.requireNonNull(applicationNaming);
        this(ApplicationNaming.of(applicationNaming));
    }

    public String name() {
        return applicationNaming.name();
    }

    public ApplicationNaming applicationNaming() {
        return applicationNaming;
    }

    public ApplicationNaming getApplicationNaming() {
        return applicationNaming;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (FromApplication) obj;
        return Objects.equals(this.applicationNaming, that.applicationNaming);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationNaming);
    }

    @Override
    public String toString() {
        return "FromApplication{" +
                "applicationNaming=" + applicationNaming +
                '}';
    }
}
