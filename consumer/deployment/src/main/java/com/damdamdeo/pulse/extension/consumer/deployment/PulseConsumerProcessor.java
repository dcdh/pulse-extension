package com.damdamdeo.pulse.extension.consumer.deployment;

import com.damdamdeo.pulse.extension.common.deployment.PulseCommonProcessor;
import com.damdamdeo.pulse.extension.common.deployment.items.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.common.deployment.items.PostgresSqlScriptBuildItem;
import com.damdamdeo.pulse.extension.common.deployment.items.ValidationErrorBuildItem;
import com.damdamdeo.pulse.extension.consumer.runtime.JacksonDecryptedPayloadToPayloadMapper;
import com.damdamdeo.pulse.extension.consumer.runtime.event.JsonNodeEventKey;
import com.damdamdeo.pulse.extension.consumer.runtime.event.JsonNodeEventKeyDeserializer;
import com.damdamdeo.pulse.extension.consumer.runtime.event.JsonNodeEventValue;
import com.damdamdeo.pulse.extension.consumer.runtime.event.JsonNodeEventValueDeserializer;
import com.damdamdeo.pulse.extension.consumer.runtime.idempotency.JdbcPostgresIdempotencyRepository;
import com.damdamdeo.pulse.extension.core.consumer.checker.SequentialEventChecker;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

import java.util.List;
import java.util.stream.Stream;

public class PulseConsumerProcessor {

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeans(final List<ValidationErrorBuildItem> validationErrorBuildItems) {
        if (validationErrorBuildItems.isEmpty()) {
            return Stream.of(
                            JdbcPostgresIdempotencyRepository.class,
                            JacksonDecryptedPayloadToPayloadMapper.class)
                    .map(beanClazz -> AdditionalBeanBuildItem.builder().addBeanClass(beanClazz).build())
                    .toList();
        } else {
            return List.of();
        }
    }

    @BuildStep
    AdditionalBeanBuildItem registerSequentialEventChecker() {
        return AdditionalBeanBuildItem.builder().addBeanClass(SequentialEventChecker.class)
                .setDefaultScope(DotNames.SINGLETON)
                .setUnremovable()
                .build();
    }

    @BuildStep
    void generateCompose(final Capabilities capabilities,
                         final ApplicationInfoBuildItem applicationInfoBuildItem,
                         final BuildProducer<ComposeServiceBuildItem> composeServiceBuildItemBuildProducer,
                         final BuildProducer<PostgresSqlScriptBuildItem> sqlScriptBuildItemProducer) {
        if (shouldGenerate(capabilities)) {
            composeServiceBuildItemBuildProducer.produce(List.of(
                    PulseCommonProcessor.POSTGRES_COMPOSE_SERVICE_BUILD_ITEM,
                    PulseCommonProcessor.KAFKA_COMPOSE_SERVICE_BUILD_ITEM));
        }
        final String schemaName = applicationInfoBuildItem.getName().toLowerCase();
        final PostgresSqlScriptBuildItem postgresSqlScriptBuildItem = new PostgresSqlScriptBuildItem(
                "%s_idempotency_consumer.sql".formatted(schemaName),
                // language=sql
                """
                        CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;
                        CREATE SCHEMA IF NOT EXISTS %1$s;
                        
                        CREATE TABLE IF NOT EXISTS %1$s.idempotency (
                          purpose character varying(255) not null,
                          from_application character varying(255) not null,
                          table_name character varying(255) not null,
                          aggregate_root_type character varying(255) not null,
                          aggregate_root_id character varying(255) not null,
                          last_consumed_version bigint not null,
                          CONSTRAINT idempotency_pkey PRIMARY KEY (purpose, from_application, table_name, aggregate_root_type, aggregate_root_id),
                          CONSTRAINT table_name_format_chk CHECK (table_name = 'EVENT' OR table_name = 'AGGREGATE_ROOT')
                        )
                        """.formatted(schemaName)
        );
        sqlScriptBuildItemProducer.produce(postgresSqlScriptBuildItem);
    }

    @BuildStep
    ReflectiveClassBuildItem reflectiveClassBuildItem() {
        return ReflectiveClassBuildItem
                .builder(JsonNodeEventValueDeserializer.class, JsonNodeEventKeyDeserializer.class,
                        JsonNodeEventKey.class, JsonNodeEventValue.class)
                .constructors().build();
    }

    public static boolean shouldGenerate(final Capabilities capabilities) {
        return !capabilities.isPresent("com.damdamdeo.pulse-writer-extension")
                && !capabilities.isPresent("com.damdamdeo.pulse-publisher-extension");
    }
}
