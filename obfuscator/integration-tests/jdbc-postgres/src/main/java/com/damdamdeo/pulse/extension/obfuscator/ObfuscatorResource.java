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

    @GET
    @Path("annotatedProjection")
    public TodoProjection getTodoProjection() {
        return new TodoProjection(TodoId.USER_1_TODO_1.id(), "lorem ipsum", Status.IN_PROGRESS, false);
    }

    @Path("deObfuscate/{todoId}")
    @GET
    public String deObfuscate(@DeObfuscate @PathParam("todoId") final String todoId) {
        return todoId;
    }
}
