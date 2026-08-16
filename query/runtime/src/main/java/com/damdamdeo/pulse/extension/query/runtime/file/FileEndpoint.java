package com.damdamdeo.pulse.extension.query.runtime.file;


import com.damdamdeo.pulse.extension.core.query.QueryException;
import com.damdamdeo.pulse.extension.core.query.file.FileContent;
import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;
import com.damdamdeo.pulse.extension.core.query.file.FileInfo;
import com.damdamdeo.pulse.extension.core.query.file.query.DownloadInput;
import com.damdamdeo.pulse.extension.core.query.file.query.DownloadQuery;
import com.damdamdeo.pulse.extension.core.query.file.query.GetFileInfoQuery;
import io.quarkiverse.resteasy.problem.HttpProblem;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.io.InputStream;

@Path("file")
public class FileEndpoint {

    @Inject
    DownloadQuery downloadQuery;

    @Inject
    GetFileInfoQuery getFileInfoQuery;

    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = HttpProblem.class)
            )
    )
    @Path("{fileIdentifier}/download")
    @GET
    public Response download(@PathParam("fileIdentifier") final FileIdentifier fileIdentifier,
                             @QueryParam("contentDisposition") @DefaultValue("INLINE") final ContentDisposition contentDisposition)
            throws QueryException {
        final FileContent content = downloadQuery.execute(new DownloadInput(fileIdentifier));
        return Response
                .ok((StreamingOutput) output -> {
                    try (final InputStream input = content.content()) {
                        input.transferTo(output);
                    }
                })
                .header("Content-Disposition", "%s; filename=\"%s.%s\"".formatted(contentDisposition.value(),
                        content.id(), content.contentType().extension()))
                .header("Content-Type", content.contentType().contentType())
                .header("Content-Length", content.contentLength().contentLength())
                .build();
    }

    @Path("{fileIdentifier}/info")
    public FileInfo getFileInfo(@PathParam("fileIdentifier") final FileIdentifier fileIdentifier) throws QueryException {
        return getFileInfoQuery.execute(fileIdentifier);
    }
}
