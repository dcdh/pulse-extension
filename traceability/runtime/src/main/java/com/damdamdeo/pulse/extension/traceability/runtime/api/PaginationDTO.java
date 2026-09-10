package com.damdamdeo.pulse.extension.traceability.runtime.api;

import com.damdamdeo.pulse.extension.core.traceability.Pagination;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

public record PaginationDTO(
        @Parameter(schema = @Schema(type = SchemaType.INTEGER), required = true)
        @QueryParam("page[index]")
        int page,
        @Parameter(schema = @Schema(type = SchemaType.INTEGER), required = true)
        @QueryParam("page[size]")
        int size) {

    public Pagination toPagination() {
        return new Pagination(page, size);
    }
}
