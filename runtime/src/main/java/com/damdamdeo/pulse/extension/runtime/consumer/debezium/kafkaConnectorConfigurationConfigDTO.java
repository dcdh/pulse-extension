package com.damdamdeo.pulse.extension.runtime.consumer.debezium;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Objects;

@RegisterForReflection
public record kafkaConnectorConfigurationConfigDTO(String schema,
                                                   String databaseHostname,
                                                   Integer databasePort,
                                                   String databaseUser,
                                                   String databasePassword,
                                                   String databaseDbname) {

    public kafkaConnectorConfigurationConfigDTO {
        Objects.requireNonNull(schema);
        Objects.requireNonNull(databaseHostname);
        Objects.requireNonNull(databasePort);
        Objects.requireNonNull(databaseUser);
        Objects.requireNonNull(databasePassword);
        Objects.requireNonNull(databaseDbname);
    }

    private kafkaConnectorConfigurationConfigDTO(final Builder builder) {
        this(builder.schema,
                builder.databaseHostname,
                builder.databasePort,
                builder.databaseUser,
                builder.databasePassword,
                builder.databaseDbname);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String schema;
        private String databaseHostname;
        private Integer databasePort;
        private String databaseUser;
        private String databasePassword;
        private String databaseDbname;

        public Builder withSchema(final String schema) {
            this.schema = schema;
            return this;
        }

        public Builder withDatabaseHostname(final String databaseHostname) {
            this.databaseHostname = databaseHostname;
            return this;
        }

        public Builder withDatabasePort(final Integer databasePort) {
            this.databasePort = databasePort;
            return this;
        }

        public Builder withDatabaseUser(final String databaseUser) {
            this.databaseUser = databaseUser;
            return this;
        }

        public Builder withDatabasePassword(final String databasePassword) {
            this.databasePassword = databasePassword;
            return this;
        }

        public Builder withDatabaseDbname(final String databaseDbname) {
            this.databaseDbname = databaseDbname;
            return this;
        }

        public kafkaConnectorConfigurationConfigDTO build() {
            return new kafkaConnectorConfigurationConfigDTO(this);
        }
    }

    @JsonProperty("connector.class")
    public String getConnectorClass() {
        return "io.debezium.connector.postgresql.PostgresConnector";
    }

    @JsonProperty("database.hostname")
    public String getDatabaseHostname() {
        return databaseHostname;
    }

    @JsonProperty("database.port")
    public String getDatabasePort() {
        return databasePort.toString();
    }

    @JsonProperty("database.user")
    public String getDatabaseUser() {
        return databaseUser;
    }

    @JsonProperty("database.password")
    public String getDatabasePassword() {
        return databasePassword;
    }

    @JsonProperty("database.dbname")
    public String getDatabaseDbname() {
        return databaseDbname;
    }

    @JsonProperty("name.include.list")
    public String getSchemaIncludeList() {
        return schema;
    }

    @JsonProperty("table.include.list")
    public String getTableIncludeList() {
        return "%s.t_event".formatted(schema);
    }

    @JsonProperty("tombstones.on.delete")
    public String getTombstonesOnDelete() {
        return "false";
    }

    @JsonProperty("key.converter")
    public String getKeyConverter() {
        return "org.apache.kafka.connect.json.JsonConverter";
    }

    @JsonProperty("value.converter")
    public String getValueConverter() {
        return "org.apache.kafka.connect.json.JsonConverter";
    }

    @JsonProperty("key.converter.schemas.enable")
    public String getKeyConverterSchemaEnable() {
        return "false";
    }

    @JsonProperty("value.converter.schemas.enable")
    public String getValueConverterSchemaEnable() {
        return "false";
    }

    @JsonProperty("topic.prefix")
    public String getTopicPrefix() {
        return schema;
    }
}
