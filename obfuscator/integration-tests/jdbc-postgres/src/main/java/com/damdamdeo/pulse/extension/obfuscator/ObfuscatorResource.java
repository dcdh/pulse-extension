package com.damdamdeo.pulse.extension.obfuscator;

import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.obfuscator.runtime.annotation.DeObfuscate;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/obfuscator")
public class ObfuscatorResource {

    @GET
    public Todo get() {
        return new Todo(TodoId.USER_1_TODO_1, "lorem ipsum", Status.IN_PROGRESS, false);
    }

    // If a 404 is returned, it means that an issue has arrise when converting from String to TodoId ... It is disturbing.
    @GET
    @Path("byAggregateId/{todoId}")
    public String getByAggregateId(@PathParam("todoId") final TodoId todoId) {
        return todoId.id();
    }

    @GET
    @Path("annotatedProjection")
    public TodoProjection getTodoProjection() {
        return new TodoProjection(TodoId.USER_1_TODO_1.id(), TodoId.USER_1_TODO_1, "lorem ipsum", Status.IN_PROGRESS, false);
    }

    @Path("deObfuscate/{todoId}")
    @GET
    public String deObfuscate(@DeObfuscate @PathParam("todoId") final String todoId) {
        return todoId;
    }

    @Path("deObfuscateByAggregateId/{todoId}")
    @GET
    public String deObfuscate(@DeObfuscate @PathParam("todoId") final TodoId todoId) {
        return todoId.id();
    }
}
