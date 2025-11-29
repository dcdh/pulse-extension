package com.damdamdeo.pulse.extension.publisher.runtime.debezium;

import com.damdamdeo.pulse.extension.common.runtime.datasource.PostgresUtils;
import com.damdamdeo.pulse.extension.core.consumer.ApplicationNaming;
import io.quarkus.agroal.runtime.DataSourcesJdbcRuntimeConfig;
import io.quarkus.arc.Unremovable;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.Validate;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
@Unremovable
public final class KafkaConnectorConfigurationGenerator {

    public static Pattern JDBC_POSTGRES_PATTERN = Pattern.compile(
            "^jdbc:postgresql://(?<host>[^/:]+):(?<port>\\d+)/(?<database>[^/]+)$");

    private final DebeziumConfiguration debeziumConfiguration;
    private final ApplicationNamingProvider applicationNamingProvider;
    private final String jdbcUrl;
    private final String datasourceUsername;
    private final String datasourcePassword;

    public KafkaConnectorConfigurationGenerator(final DebeziumConfiguration debeziumConfiguration,
                                                final ApplicationNamingProvider applicationNamingProvider,
                                                final DataSourcesJdbcRuntimeConfig dataSourcesJdbcRuntimeConfig,
                                                final DataSourcesRuntimeConfig dataSourcesRuntimeConfig) {
        jdbcUrl = dataSourcesJdbcRuntimeConfig.dataSources().get(DataSourceUtil.DEFAULT_DATASOURCE_NAME).jdbc().url()
                .orElseThrow(() -> new IllegalArgumentException("quarkus.datasource.jdbc.url is mandatory"));
        datasourceUsername = dataSourcesRuntimeConfig.dataSources().get(DataSourceUtil.DEFAULT_DATASOURCE_NAME).username()
                .orElseThrow(() -> new IllegalArgumentException("quarkus.datasource.username is mandatory"));
        datasourcePassword = dataSourcesRuntimeConfig.dataSources().get(DataSourceUtil.DEFAULT_DATASOURCE_NAME).password()
                .orElseThrow(() -> new IllegalArgumentException("quarkus.datasource.password is mandatory"));
        this.debeziumConfiguration = Objects.requireNonNull(debeziumConfiguration);
        this.applicationNamingProvider = Objects.requireNonNull(applicationNamingProvider);
    }

    public KafkaConnectorConfigurationDTO generateConnectorConfiguration() {
        final ApplicationNaming applicationNaming = applicationNamingProvider.provide();
        final Matcher matcher = KafkaConnectorConfigurationGenerator.JDBC_POSTGRES_PATTERN.matcher(jdbcUrl);
        Validate.validState(matcher.matches(), "quarkus.datasource.jdbc.url '%s' is invalid".formatted(jdbcUrl));
        final String host = "localhost".equals(matcher.group("host")) ? PostgresUtils.SERVICE_NAME : matcher.group("host");
        final int port = "localhost".equals(matcher.group("host")) ? PostgresUtils.DEFAULT_PORT : Integer.parseInt(matcher.group("port"));
        final String database = matcher.group("database");
        return KafkaConnectorConfigurationDTO
                .newBuilder()
                .withName(applicationNaming.value().toLowerCase())
                .withConfig(
                        kafkaConnectorConfigurationConfigDTO
                                .newBuilder()
                                .withSchema(applicationNaming.value().toLowerCase())
                                .withDatabaseHostname(host)
                                .withDatabasePort(port)
                                .withDatabaseUser(datasourceUsername)
                                .withDatabasePassword(datasourcePassword)
                                .withDatabaseDbname(database)
                                .build())
                .build();
    }
}
