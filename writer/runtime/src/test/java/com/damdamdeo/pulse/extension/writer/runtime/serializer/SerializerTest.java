package com.damdamdeo.pulse.extension.writer.runtime.serializer;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.event.Event;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class SerializerTest {

    static class TodoMixin {

        @JsonCreator
        public TodoMixin(
                @JsonProperty("id") TodoId var1,
                @JsonProperty("description") String var2,
                @JsonProperty("status") Status var3,
                @JsonProperty("important") boolean var4
        ) {
        }
    }

    static class TodoIdMixin {

        @JsonCreator
        public TodoIdMixin(
                @JsonProperty("user") String var1,
                @JsonProperty("sequence") Long var2
        ) {
        }
    }

    static class TodoChecklistMixin {

        @JsonCreator
        public TodoChecklistMixin(
                @JsonProperty("id") TodoChecklistId var1,
                @JsonProperty("description") String var2
        ) {
        }
    }

    static class TodoChecklistIdMixin {

        @JsonCreator
        public TodoChecklistIdMixin(
                @JsonProperty("todoId") TodoId var1,
                @JsonProperty("index") Long var2
        ) {
        }
    }

    static final class FullTodo extends AggregateRoot<TodoId> {

        private String description;
        private Status status;
        private boolean important = false;
        private List<TodoChecklist> todoChecklistList = new ArrayList<>();

        public FullTodo(final TodoId id) {
            super(id);
        }

        public FullTodo(final TodoId id,
                        final String description,
                        final Status status,
                        final boolean important,
                        final List<TodoChecklist> todoChecklistList) {
            super(id);
            this.description = description;
            this.status = status;
            this.important = important;
            this.todoChecklistList = todoChecklistList;
        }

        @Override
        public BelongsTo belongsTo() {
            throw new IllegalStateException("Should not be called");
        }

        @Override
        public OwnedBy ownedBy() {
            throw new IllegalStateException("Should not be called");
        }
    }

    static class FullTodoMixin {

        @JsonCreator
        public FullTodoMixin(
                @JsonProperty("id") TodoId var1,
                @JsonProperty("description") String var2,
                @JsonProperty("status") Status var3,
                @JsonProperty("important") boolean var4,
                @JsonProperty("todoChecklistList") List<TodoChecklist> var5
        ) {
        }
    }

    public record NombreDeConvives(Integer nombre) {

        public NombreDeConvives {
            Objects.requireNonNull(nombre);
        }
    }

    public record CommandeEnCoursDePrise(NombreDeConvives nombreDeConvives) implements Event {

        public CommandeEnCoursDePrise {
            Objects.requireNonNull(nombreDeConvives);
        }
    }

    static class NombreDeConvivesMixIn {
        @JsonCreator
        public NombreDeConvivesMixIn(@JsonProperty("nombre") Integer var1) {

        }
    }

    static class CommandeEnCoursDePriseMixIn {
        @JsonCreator
        public CommandeEnCoursDePriseMixIn(@JsonProperty("nombreDeConvives") NombreDeConvives var1) {

        }
    }

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.addMixIn(Todo.class, TodoMixin.class);
        OBJECT_MAPPER.addMixIn(TodoId.class, TodoIdMixin.class);
        OBJECT_MAPPER.addMixIn(TodoChecklist.class, TodoChecklistMixin.class);
        OBJECT_MAPPER.addMixIn(TodoChecklistId.class, TodoChecklistIdMixin.class);
        OBJECT_MAPPER.addMixIn(FullTodo.class, FullTodoMixin.class);
        OBJECT_MAPPER.addMixIn(NombreDeConvives.class, NombreDeConvivesMixIn.class);
        OBJECT_MAPPER.addMixIn(CommandeEnCoursDePrise.class, CommandeEnCoursDePriseMixIn.class);
    }

    @Test
    void shouldDeserializedTodo() throws JsonProcessingException {
        // Given
        // language=json
        final String givenContent = """
                {
                  "id": {
                    "user": "Damien",
                    "sequence": 14
                  },
                  "description": "lorem ipsum",
                  "status": "DONE",
                  "important": false
                }
                """;

        // When
        final Todo deserializedTodo = OBJECT_MAPPER.readValue(givenContent, Todo.class);

        // Then
        assertThat(deserializedTodo).isEqualTo(new Todo(
                new TodoId("Damien", 14L),
                "lorem ipsum",
                Status.DONE,
                false
        ));
    }

    @Test
    void shouldDeserializedFullTodo() throws JsonProcessingException {
        // Given
        // language=json
        final String givenContent = """
                {
                  "id": {
                    "user": "Damien",
                    "sequence": 14
                  },
                  "description": "lorem ipsum",
                  "status": "DONE",
                  "important": false,
                  "todoChecklistList": [
                    {
                      "id": {
                        "todoId": {
                          "user": "Damien",
                          "sequence": 14
                        },
                        "index": 0
                      },
                      "description": "Implement Projection feature"
                    },
                    {
                      "id": {
                        "todoId": {
                          "user": "Damien",
                          "sequence": 14
                        },
                        "index": 1
                      },
                      "description": "Organization vacancies"                      
                    }
                  ]
                }
                """;

        // When
        final FullTodo deserializedFullTodo = OBJECT_MAPPER.readValue(givenContent, FullTodo.class);

        // Then
        assertThat(deserializedFullTodo).isEqualTo(new FullTodo(
                new TodoId("Damien", 14L),
                "lorem ipsum",
                Status.DONE,
                false,
                List.of(
                        new TodoChecklist(
                                new TodoChecklistId(new TodoId("Damien", 14L), 0L),
                                "Implement Projection feature"
                        ),
                        new TodoChecklist(
                                new TodoChecklistId(new TodoId("Damien", 14L), 1L),
                                "Organization vacancies"
                        )
                )
        ));
    }

    @Test
    void toto() throws JsonProcessingException {
        // System.out.println(OBJECT_MAPPER.writeValueAsString(new CommandeEnCoursDePrise(new NombreDeConvives(10))));
        CommandeEnCoursDePrise commandeEnCoursDePrise = OBJECT_MAPPER.readValue(
                """
                        {"nombreDeConvives":{"nombre":10}}
                        """, CommandeEnCoursDePrise.class);
        System.out.print(commandeEnCoursDePrise);
    }
}
