package com.damdamdeo.pulse.extension.publisher.runtime.debezium;

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

    @JsonProperty("name")
    public String getName() {
        return schema;
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

    @JsonProperty("schema.include.list")
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
        return "pulse";
    }

    @JsonProperty("plugin.name")
    public String getPluginName() {
        return "pgoutput";
    }

    @JsonProperty("transforms")
    public String getTransforms() {
        return "unwrap,filterFields,partitioner";
    }

    @JsonProperty("transforms.unwrap.type")
    public String getTransformsUnwrapType() {
        return "io.debezium.transforms.ExtractNewRecordState";
    }

    @JsonProperty("transforms.unwrap.drop.tombstones")
    public String getTransformsUnwrapDropTombstones() {
        return "false";
    }

    @JsonProperty("transforms.unwrap.delete.handling.mode")
    public String getTransformsUnwrapDeleteHandlingMode() {
        return "drop";
    }

    @JsonProperty("transforms.unwrap.operation.header")
    public String getTransformsUnwrapOperationHeader() {
        return "true";
    }

    @JsonProperty("transforms.unwrap.add.headers")
    public String getTransformsUnwrapAddHeaders() {
        return "source.version,source.connector,source.name,source.ts_ms,source.db,source.schema,source.table,source.txId,source.lsn";
    }

    @JsonProperty("transforms.filterFields.type")
    public String getTransformsFilterFieldsType() {
        return "org.apache.kafka.connect.transforms.ReplaceField$Value";
    }

    @JsonProperty("transforms.filterFields.include")
    public String getTransformsFilterFieldsInclude() {
        return "creation_date,event_type,event_payload,owned_by,in_relation_with";
    }

    @JsonProperty("transforms.partitioner.type")
    public String getTransformsPartitionerType() {
        return "io.debezium.transforms.partitions.PartitionRouting";
    }

    @JsonProperty("transforms.partitioner.partition.payload.fields")
    public String getTransformsPartitionerPartitionPayloadFields() {
        return "in_relation_with";
    }

    @JsonProperty("transforms.partitioner.partition.topic.num")
    public Integer getTransformsPartitionerPartitionTopicNum() {
        return 1;
    }

    @JsonProperty("compression.type")
    public String getCompressionType() {
        return "zstd";
    }

    @JsonProperty("topic.creation.default.replication.factor")
    public Integer getTopicCreationDefaultReplicationFactor() {
        return 1;
    }

    @JsonProperty("topic.creation.default.partitions")
    public Integer getTopicCreationDefaultPartitions() {
        return 1;
    }

    @JsonProperty("topic.creation.default.cleanup.policy")
    public String getTopicCreationDefaultCleanupPolicy() {
        return "compact";
    }

    @JsonProperty("topic.creation.default.compression.type")
    public String getTopicCreationDefaultCompressionType() {
        return "zstd";
    }
}
