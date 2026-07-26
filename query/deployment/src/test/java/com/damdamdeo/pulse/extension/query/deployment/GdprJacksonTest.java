package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.TodoChecklistId;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.query.runtime.gdpr.encryptable.Encryptable;
import com.damdamdeo.pulse.extension.query.runtime.gdpr.hashable.Hashable;
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

import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class GdprJacksonTest {
    // FCK
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(StubPassphraseProvider.class))
            .withConfigurationResource("application.properties");

    record Username(String username) implements Hashable, Encryptable {

        @Override
        public String value() {
            return username;
        }
    }

    record Description(String description) implements Encryptable {

        @Override
        public String value() {
            return description;
        }
    }

    record GdprTodo(TodoId todoId,
                    Username username,
                    Description description,
                    Status status,
                    boolean important,
                    List<GdprChecklist> gdprChecklists,
                    OwnedBy ownedBy) {

        GdprTodo {
//            Objects.requireNonNull(todoId);
//            Objects.requireNonNull(username);
//            Objects.requireNonNull(description);
//            Objects.requireNonNull(status);
//            Objects.requireNonNull(gdprChecklists);
//            Objects.requireNonNull(ownedBy);
        }
    }

    record GdprChecklist(TodoChecklistId todoChecklistId,
                         Description description) {

        GdprChecklist {
//            Objects.requireNonNull(todoChecklistId);
//            Objects.requireNonNull(description);
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
              "username_hash" : "2219fcb74e34d2f6fbedc545ac8ca4adcb908bb3a703aedc42af6c3f66510784",
              "username_encrypted": "wx4EBwMCb0QgWRgJLiJgNrNoy4DIjg+rDS90DcNmgBLSQgEMp1rDBMJXtHZG1dYArv8secMSPeDtx3/MCpVaH0TCMaXDq+137aaE+p+EgZj6UUe6YjBtF+vkdCCzBCnmQ4R5ww==",
              "description_encrypted": "wx4EBwMC5Xu0vPqXUM1gNBQHf8gQFXKgcWHh1fkT+HfSQAHQrCg7IKKlOY2tN7Rg9DLadbk9B8XrDwfhukR+cgHkJqOPp6jH3A7R6pQohlc3TdngBW/m0NeR8R/oZXBJTFY=",
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
                  "description_encrypted": "wx4EBwMCzJdMVWZygCNgoRNE49nsbt5jZPOp0plK7R3SUQGHlXDTAZYNG4OGkTNaCxbtD+SGVfnol+xjpzsWQ6JJzWIisKMFmZvX1aeh7ZSeBR8Q2zbBBljjH6HYAfm0KcdXnilfooXquDHS1HN9wVNDPw=="
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
                  "description_encrypted": "wx4EBwMCOUj3mneinPBgtZxufJDOBb2NvUc4xP3XT63SSwFIJ8nsR/v5xQo3zIPxhKoXi7rlt2ffr2zM+4LRpdabHHOt1QQ7FKQ3faersByrB4xJJQnOUe7Yqsn0Z/VLdJ1r7N691PnLcH4ujQ=="
                }
              ],
              "ownedBy": "U000001-T000001"
            }
            """;

    @Inject
    ObjectMapper objectMapper;

    @Test
    void shouldSerialize() throws JsonProcessingException, JSONException {
        // Given
        final GdprTodo givenGdprTodo = new GdprTodo(
                TodoId.USER_1_TODO_1,
                new Username("bob@gmail.com"),
                new Description("lorem ipsum"),
                Status.DONE,
                false,
                List.of(
                        new GdprChecklist(
                                TodoChecklistId.USER_1_TODO_1_1, new Description("Implement Projection feature")),
                        new GdprChecklist(
                                TodoChecklistId.USER_1_TODO_1_2, new Description("Organization vacancies"))
                ),
                OwnedBy.from(TodoId.USER_1_TODO_1));

        // When
        final String serialized = objectMapper.writeValueAsString(givenGdprTodo);

        // Then
        System.out.println(serialized);
        final Pattern encryptedPattern = Pattern.compile("^[A-Za-z0-9+/=]+$");
        JSONAssert.assertEquals(SERIALIZED, serialized,
                new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("username_encrypted",
                                (expected, actual) -> encryptedPattern.matcher(actual.toString()).matches()),
                        new Customization("description_encrypted",
                                (expected, actual) -> encryptedPattern.matcher(actual.toString()).matches()),
                        new Customization("gdprChecklists[*].description_encrypted",
                                (expected, actual) -> encryptedPattern.matcher(actual.toString()).matches())
                ));
    }

    @Test
    void shouldDeserialize() throws JsonProcessingException {
        // Given

        // When
        final GdprTodo gdprTodo = objectMapper.readValue(SERIALIZED, GdprTodo.class);

        // Then
        assertThat(gdprTodo).isEqualTo(new GdprTodo(
                TodoId.USER_1_TODO_1,
                new Username("bob@gmail.com"),
                new Description("lorem ipsum"),
                Status.DONE,
                false,
                List.of(
                        new GdprChecklist(
                                TodoChecklistId.USER_1_TODO_1_1, new Description("Implement Projection feature")),
                        new GdprChecklist(
                                TodoChecklistId.USER_1_TODO_1_2, new Description("Organization vacancies"))
                ),
                OwnedBy.from(TodoId.USER_1_TODO_1)));
    }
}
