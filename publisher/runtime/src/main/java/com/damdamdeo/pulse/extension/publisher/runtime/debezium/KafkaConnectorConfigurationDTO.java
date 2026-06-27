package com.damdamdeo.pulse.extension.publisher.runtime.debezium;

import com.damdamdeo.pulse.extension.core.consumer.SchemaName;
import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record KafkaConnectorConfigurationDTO(String name, kafkaConnectorConfigurationConfigDTO config) {

    public KafkaConnectorConfigurationDTO {
        Objects.requireNonNull(name);
        Objects.requireNonNull(config);
    }

    public KafkaConnectorConfigurationDTO(final Builder builder) {
        this(builder.name, builder.config);
        Validate.validState(builder.name.equals(builder.config.getName()), "Name and config name must be the same using schema name");
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private kafkaConnectorConfigurationConfigDTO config;

        public Builder withName(final SchemaName schemaName) {
            this.name = schemaName.name();
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
