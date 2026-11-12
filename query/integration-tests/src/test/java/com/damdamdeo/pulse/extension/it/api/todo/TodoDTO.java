package com.damdamdeo.pulse.extension.it.api.todo;

import com.damdamdeo.pulse.extension.core.Description;
import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.Todo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;

public record TodoDTO(String todoId,
                      @JsonSerialize(converter = DescriptionToString.class)
                      @JsonDeserialize(converter = StringToDescription.class)
                      Description description,
                      Status status,
                      boolean important,
                      boolean inProgress,
                      String ownerId) {

    public TodoDTO(final Todo todo) {
        this(todo.todoId().id(), todo.description(), todo.status(), todo.important(),
                todo.isInProgress(), todo.owner().id().id());
    }

    public static class DescriptionToString extends StdConverter<Description, String> {

        @Override
        public String convert(final Description value) {
            return value.description();
        }
    }

    public static class StringToDescription extends StdConverter<String, Description> {

        @Override
        public Description convert(final String value) {
            return new Description(value);
        }
    }
}
