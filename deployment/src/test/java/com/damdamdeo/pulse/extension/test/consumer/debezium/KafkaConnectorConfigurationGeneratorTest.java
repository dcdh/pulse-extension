package com.damdamdeo.pulse.extension.test.consumer.debezium;

import com.damdamdeo.pulse.extension.runtime.consumer.debezium.KafkaConnectorConfigurationGenerator;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

class KafkaConnectorConfigurationGeneratorTest {

    @Inject
    KafkaConnectorConfigurationGenerator kafkaConnectorConfigurationGenerator;

    @Test
    void todo() {
        throw new RuntimeException("TODO");

        passage par JsonAssert ...
        je dois tester que c'est valide

        POST /connector-plugins/FileStreamSinkConnector/config/validate
        Content-Type: application/json

        {
            "config": {
            "file": "/tmp/test.sink.txt",
                    "topics": "test-topic"
        }
        }

    }
}
//FCK 3 il y a un endpoint connect qui test la validité de la conf ... faire un test ko en premier puis ok
//je dois passer par le generator
