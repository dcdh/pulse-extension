/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.damdamdeo.pulse.extension.it;

import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.runtime.consumer.EventChannel;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Optional;

@Path("/pulse-extension")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class PulseExtensionResource {

    // https://github.com/quarkusio/quarkus/issues/19676
    @Inject
    @CacheName("passphrase")
    Cache cache;

    @Inject
    CommandHandler<Todo, TodoId> todoCommandHandler;

    @Inject
    @Any
    Instance<StatisticsEventHandler> statisticsEventHandlerInstance;

    public record TodoDTO(String id, String description, Status status, boolean important) {

        public static TodoDTO from(final Todo todo) {
            return new TodoDTO(
                    todo.id().id(),
                    todo.description(),
                    todo.status(),
                    todo.important()
            );
        }
    }

    @POST
    @Path("/createTodo")
    public TodoDTO createTodo() {
        return TodoDTO.from(todoCommandHandler.handle(new CreateTodo(new TodoId("Damien", 20L), "lorem ipsum")));
    }

    @POST
    @Path("/called")
    public Call getCall() {
        final Call call = statisticsEventHandlerInstance
                .select(EventChannel.Literal.of("statistics")).get().getCall();
        return Optional.ofNullable(call)
                .orElseThrow(() -> new NotFoundException("Call not executed yet"));
    }
}
