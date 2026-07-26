package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.TodoChecklistId;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.query.runtime.gdpr.Sensitive;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class GdprJacksonSerializationTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(StubPassphraseProvider.class))
            .withConfigurationResource("application.properties");

    record GdprTodo(TodoId todoId,
                    @Sensitive(Sensitive.Mode.ENCRYPT)
                    LocalDateTime createdAt,
                    @Sensitive(Sensitive.Mode.ENCRYPT_AND_SEARCH_BY_HASH)
                    String username,
                    @Sensitive(Sensitive.Mode.ENCRYPT)
                    String description,
                    Status status,
                    boolean important,
                    List<GdprChecklist> gdprChecklists,
                    @Sensitive(Sensitive.Mode.ENCRYPT_AND_SEARCH_BY_HASH)
                    OwnedBy ownedBy,
                    @Sensitive(Sensitive.Mode.ENCRYPT)
                    Long version) {

        GdprTodo {
            Objects.requireNonNull(todoId);
            Objects.requireNonNull(username);
            Objects.requireNonNull(description);
            Objects.requireNonNull(status);
            Objects.requireNonNull(gdprChecklists);
            Objects.requireNonNull(ownedBy);
        }
    }

    record GdprChecklist(TodoChecklistId todoChecklistId,
                         @Sensitive(Sensitive.Mode.ENCRYPT)
                         String description) {

        GdprChecklist {
            Objects.requireNonNull(todoChecklistId);
            Objects.requireNonNull(description);
        }
    }

    // language=json
    public static final String SERIALIZED = """
            {
              "todoId": {
                "userId": {
                  "sequence": "000001"
                },
                "sequence": "000001"
              },
              "createdAt_encrypted": "wx4EBwMC1YRW/9fjArNgyZMJ0BFExu56idO34YL1GAnSSAEf3/iAa7kAfMXaPpLkfCtwxAAjZrhbVxpQh5NtG0WCdS3BFG/3MpcvGirRxTQ+oD637sxDBX3JKr9xGltPwWo2FpL5gcg0zA==",
              "username_hash": "2219fcb74e34d2f6fbedc545ac8ca4adcb908bb3a703aedc42af6c3f66510784",
              "username_encrypted": "wx4EBwMCdT9oHrynoeNgD2aq3kqS0B+gWS47pcRAV5fSQgGmqgMpbjyDhhz7pDZSaTjmlhNLZpN5y7ZuTbB/4NRn204atj9aLv9nVe9hGcG1sR/NJeCwawiXeHoWmDNO6v6LeA==",
              "description_encrypted": "wx4EBwMC98RmEqVdnAdg78dAm73XkL+D7M3OonVgHT7SQAFlFC+MRMfd0H0GyvdJJqaAzqcl42LlfEl5GdOjvHVSHzjdDgcwVYMtWMNSgWA5mYhodqHh/tVDA07cuAsCd60=",
              "status": "DONE",
              "important": false,
              "gdprChecklists": [
                {
                  "todoChecklistId": {
                    "todoId": {
                      "userId": {
                        "sequence": "000001"
                      },
                      "sequence": "000001"
                    },
                    "sequence": "000001"
                  },
                  "description_encrypted": "wx4EBwMChratjd+WfN5g0guOUWBMWu93Nq5ewMcDPIfSYwGDrF/kio33USyDc7H9+4aF5ys4l6eXIs8+/0QMB5f/VrWdb1szs4P5Dncozd/Asr6b5OawE4kNrvMybvmzI4N6sbb3Ro+UVIQM/wglZRhigJMbnHqi0R8du5lRHPk2wiXeCA=="
                },
                {
                  "todoChecklistId": {
                    "todoId": {
                      "userId": {
                        "sequence": "000001"
                      },
                      "sequence": "000001"
                    },
                    "sequence": "000002"
                  },
                  "description_encrypted": "wx4EBwMCx4Q+1LexMXVgWs3xo3j9y6brPX9N6eZMu2zSSwGIVME0fAkLX382T4MpMsoLLHu4q8JILGmmBsVUFs9jMCSC8Tjp5MVpOSGgl2JO1/5BBlwMV3sRBCLP3ktrVzNDsC0lulHqPiiJ4w=="
                }
              ],
              "ownedBy_hash": "942802553d3572c00f8186bb7db4332e261b39e11e632faf79981730ded9e1cd",
              "ownedBy_encrypted": "wx4EBwMC1PmO+MO/Zg5gZuqRdsq6I7NpQpvvKulA0R7SRAGRUVloOLFGoIQxSUkYMnxxagUa+kPFas5Ak63FbOLV0vSWil7HGbIQLodGF0k5USgCVET3o6x2BRLw+kV8x0h+JKrA",
              "version_encrypted": "wx4EBwMCUNxSnFsm+1Bg3906Tp+A/kfTzN6IQheBc6/SNgHDpXhCAZtxF7IL5NUrOYmiTzz4HMh15NIzvLBW9ey+87sfY8hMvSD2OQcrrL1nS3XOePgo5w=="
            }
            """;

    @Inject
    ObjectMapper objectMapper;

    private static GdprTodo GIVEN_GDPR_TODO = new GdprTodo(
            TodoId.USER_1_TODO_1,
            LocalDateTime.of(2026, 7, 26, 20, 47, 15),
            "bob@gmail.com",
            "lorem ipsum",
            Status.DONE,
            false,
            List.of(
                    new GdprChecklist(
                            TodoChecklistId.USER_1_TODO_1_1, "Analyse features\nImplement Projection feature"),
                    new GdprChecklist(
                            TodoChecklistId.USER_1_TODO_1_2, "Organization vacancies")
            ),
            OwnedBy.from(TodoId.USER_1_TODO_1),
            2L);

    private static GdprTodo EXPECTED_GDPR_TODO = new GdprTodo(
            TodoId.USER_1_TODO_1,
            LocalDateTime.of(2026, 7, 26, 20, 47, 15),
            "bob@gmail.com",
            "lorem ipsum",
            Status.DONE,
            false,
            List.of(
                    new GdprChecklist(
                            // json protect \n which is normal - json does not support multiline String
                            TodoChecklistId.USER_1_TODO_1_1, "Analyse features\\nImplement Projection feature"),
                    new GdprChecklist(
                            TodoChecklistId.USER_1_TODO_1_2, "Organization vacancies")
            ),
            OwnedBy.from(TodoId.USER_1_TODO_1),
            2L);

    @Test
    void shouldSerialize() throws JsonProcessingException, JSONException {
        // Given

        // When
        final String serialized = objectMapper.writeValueAsString(GIVEN_GDPR_TODO);

        // Then
        System.out.println(serialized);
        final Pattern encryptedPattern = Pattern.compile("^[A-Za-z0-9+/=]+$");
        JSONAssert.assertEquals(SERIALIZED, serialized,
                new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("createdAt_encrypted",
                                (expected, actual) -> encryptedPattern.matcher(actual.toString()).matches()),
                        new Customization("username_encrypted",
                                (expected, actual) -> encryptedPattern.matcher(actual.toString()).matches()),
                        new Customization("description_encrypted",
                                (expected, actual) -> encryptedPattern.matcher(actual.toString()).matches()),
                        new Customization("gdprChecklists[*].description_encrypted",
                                (expected, actual) -> encryptedPattern.matcher(actual.toString()).matches()),
                        new Customization("version_encrypted",
                                (expected, actual) -> encryptedPattern.matcher(actual.toString()).matches()),
                        new Customization("ownedBy_encrypted",
                                (expected, actual) -> encryptedPattern.matcher(actual.toString()).matches())
                ));
    }

    @Test
    void shouldDeserialize() throws JsonProcessingException {
        // Given

        // When
        final GdprTodo gdprTodo = objectMapper.readValue(SERIALIZED, GdprTodo.class);

        // Then
        assertThat(gdprTodo).isEqualTo(EXPECTED_GDPR_TODO);
    }

    @Test
    void shouldSerializeDeserialize() throws JsonProcessingException {
        // Given

        // When
        final String serialized = objectMapper.writeValueAsString(GIVEN_GDPR_TODO);
        final GdprTodo gdprTodo = objectMapper.readValue(serialized, GdprTodo.class);

        // Then
        assertThat(gdprTodo).isEqualTo(EXPECTED_GDPR_TODO);
    }
}
