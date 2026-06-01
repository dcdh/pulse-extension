package com.damdamdeo.pulse.extension.kafka.deployment;

import com.damdamdeo.pulse.extension.compose.deployment.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.compose.runtime.datasource.KafkaUtils;
import com.damdamdeo.pulse.extension.compose.runtime.datasource.PostgresUtils;

import java.util.List;
import java.util.Map;

public class KafkaProcessor {

    public static ComposeServiceBuildItem.ServiceName KAFKA_SERVICE_NAME = new ComposeServiceBuildItem.ServiceName(KafkaUtils.SERVICE_NAME);

    public static ComposeServiceBuildItem.ServiceName POSTGRES_SERVICE_NAME = new ComposeServiceBuildItem.ServiceName(PostgresUtils.SERVICE_NAME);

    public static ComposeServiceBuildItem KAFKA_COMPOSE_SERVICE_BUILD_ITEM = new ComposeServiceBuildItem(
            KAFKA_SERVICE_NAME,
            new ComposeServiceBuildItem.ImageName("debezium-for-dev-service/kafka:4.2.0"),
            new ComposeServiceBuildItem.Labels(
                    Map.of("io.quarkus.devservices.compose.exposed_ports", "/tmp/ports")),
            new ComposeServiceBuildItem.Ports(List.of("9092", "29092")),
            ComposeServiceBuildItem.Links.ofNone(),
            new ComposeServiceBuildItem.EnvironmentVariables(
                    Map.of("CLUSTER_ID", "oh-sxaDRTcyAr6pFRbXyzA",
                            "NODE_ID", "1",
                            "KAFKA_CONTROLLER_QUORUM_VOTERS", "1@kafka:9093")),
            ComposeServiceBuildItem.Command.ofNone(),
            ComposeServiceBuildItem.Entrypoint.ofNone(),
            null,
            List.of(
// Not working for executable
// Even if `srcResolved.toFile().setExecutable(true);`
// Using KAFKA_LISTENERS=PLAINTEXT://172.24.0.3:9092,CONTROLLER://172.24.0.3:9093 and KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://172.24.0.3:9092
// /docker-entrypoint.sh: line 278: /kafka.sh: Permission denied
// Starting in KRaft mode, using CLUSTER_ID=oh-sxaDRTcyAr6pFRbXyzA, NODE_ID=1 and NODE_ROLE=combined.
// Using configuration config/server.properties.
// Using KAFKA_LISTENERS=PLAINTEXT://172.24.0.3:9092,CONTROLLER://172.24.0.3:9093 and KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://172.24.0.3:9092
// /docker-entrypoint.sh: line 278: /kafka.sh: Permission denied
//                    new ComposeServiceBuildItem.Volume("./kafka.sh", "/kafka.sh",
//                            // language=bash
//                            """
//                                    #!/bin/bash
//                                    set -euxo pipefail;
//                                    while [ ! -f /tmp/ports ]; do
//                                      sleep 0.1;
//                                    done;
//                                    sleep 0.1;
//                                    source /tmp/ports;
//                                    export KAFKA_LISTENERS=INTERNAL://0.0.0.0:29092,EXTERNAL://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093;
//                                    export KAFKA_ADVERTISED_LISTENERS=INTERNAL://kafka:29092,EXTERNAL://localhost:$PORT_9092;
//                                    export KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT;
//                                    export KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER;
//                                    export KAFKA_INTER_BROKER_LISTENER_NAME=INTERNAL;
//                                    exec /docker-entrypoint.sh start
//                                    """.getBytes(StandardCharsets.UTF_8))
            ),
            ComposeServiceBuildItem.DependsOn.on(List.of(POSTGRES_SERVICE_NAME)));

}
