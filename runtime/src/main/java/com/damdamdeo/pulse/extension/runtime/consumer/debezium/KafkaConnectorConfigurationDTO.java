package com.damdamdeo.pulse.extension.runtime.consumer.debezium;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Objects;

@RegisterForReflection
public record KafkaConnectorConfigurationDTO(String name, kafkaConnectorConfigurationConfigDTO config) {

    public KafkaConnectorConfigurationDTO {
        Objects.requireNonNull(name);
        Objects.requireNonNull(config);
    }

    public KafkaConnectorConfigurationDTO(final Builder builder) {
        this(builder.name, builder.config);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private kafkaConnectorConfigurationConfigDTO config;

        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public Builder withConfig(final kafkaConnectorConfigurationConfigDTO config) {
            this.config = config;
            return this;
        }

        public KafkaConnectorConfigurationDTO build() {
            return new KafkaConnectorConfigurationDTO(this);
        }

    }
}
