package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.build.report.deployment.ContentBuildItem;
import com.damdamdeo.pulse.extension.build.report.deployment.content.CodeBlock;
import com.damdamdeo.pulse.extension.build.report.deployment.content.Content;
import com.damdamdeo.pulse.extension.build.report.deployment.content.Title;
import com.damdamdeo.pulse.extension.common.deployment.items.EligibleTypeForSerializationBuildItem;
import com.damdamdeo.pulse.extension.core.Nullable;
import com.damdamdeo.pulse.extension.core.SequenceNumber;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.apache.commons.lang3.tuple.Pair;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static com.damdamdeo.pulse.extension.common.deployment.SerializerProcessor.isAbstractClass;

public class SerializerProcessor {

    @BuildStep
    public List<ContentBuildItem> generateSerializationObjectsReport(final List<EligibleTypeForSerializationBuildItem> eligibleTypeForSerializationBuildItems,
                                                                     final CombinedIndexBuildItem indexItem) {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // Cf. BusinessObjectMapperProducer
        final SchemaGeneratorConfigBuilder schemaGeneratorConfigBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
        schemaGeneratorConfigBuilder
                .without(Option.PUBLIC_STATIC_FIELDS)
                .without(Option.STATIC_METHODS)
                .without(Option.NONPUBLIC_STATIC_FIELDS)
                .without(Option.FLATTENED_ENUMS_FROM_TOSTRING)
                .without(Option.GETTER_METHODS);
        schemaGeneratorConfigBuilder.forTypesInGeneral()
                .withCustomDefinitionProvider((javaType, context) -> {
                    if (javaType.getErasedType() == SequenceNumber.class) {
                        final ObjectNode definition = context.createDefinition(
                                context.getTypeContext().resolve(Long.class));
                        return new CustomDefinition(definition);
                    }
                    return null;
                });

        schemaGeneratorConfigBuilder
                .forFields().withRequiredCheck(field -> field.getAnnotationConsideringFieldAndGetterIfSupported(Nullable.class) == null);
        final SchemaGeneratorConfig config = schemaGeneratorConfigBuilder.build();

        final SchemaGenerator generator = new SchemaGenerator(config);
        final ObjectMapper objectMapper = new ObjectMapper();

        final IndexView index = indexItem.getIndex();
        return Stream.concat(
                Stream.of(new Title(Title.Level.SECOND, "Json Schema Definitions")),
                eligibleTypeForSerializationBuildItems.stream()
                        .flatMap(eligibleTypeForSerializationBuildItem -> {
                            final List<Content> contents = new ArrayList<>();
                            contents.add(new Title(Title.Level.THIRD, eligibleTypeForSerializationBuildItem.clazz().getSimpleName() + "s"));
                            final List<ClassInfo> classesInfoToGenerateJsonSchema = new ArrayList<>();
                            if (eligibleTypeForSerializationBuildItem.clazz().isInterface()) {
                                final Collection<ClassInfo> clazzes = index.getAllKnownImplementations(DotName.createSimple(eligibleTypeForSerializationBuildItem.clazz()));
                                classesInfoToGenerateJsonSchema.addAll(clazzes);
                            } else if (isAbstractClass(eligibleTypeForSerializationBuildItem.clazz())) {
                                final Collection<ClassInfo> clazzes = index.getAllKnownSubclasses(DotName.createSimple(eligibleTypeForSerializationBuildItem.clazz()));
                                classesInfoToGenerateJsonSchema.addAll(clazzes);
                            }
                            return Stream.concat(contents.stream(),
                                    classesInfoToGenerateJsonSchema.stream()
                                            .map(classInfoToGenerateJsonSchema -> {
                                                try {
                                                    return classLoader.loadClass(classInfoToGenerateJsonSchema.name().toString());
                                                } catch (final ClassNotFoundException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            })
                                            .map(clazz -> {
                                                try {
                                                    return Pair.of(clazz, generator.generateSchema(clazz));
                                                } catch (final Exception exception) {
                                                    throw new RuntimeException(exception);
                                                }
                                            })
                                            .flatMap(jsonSchema -> {
                                                try {
                                                    return Stream.of(
                                                            new Title(Title.Level.FOURTH, jsonSchema.getKey().getSimpleName()),
                                                            CodeBlock.fromJson(jsonSchema.getValue(), objectMapper));
                                                } catch (final JsonProcessingException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            }));
                        })
        ).map(ContentBuildItem::new).toList();
    }
}
