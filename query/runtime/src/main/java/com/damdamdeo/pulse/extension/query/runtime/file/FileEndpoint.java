package com.damdamdeo.pulse.extension.query.runtime.file;


import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import com.damdamdeo.pulse.extension.core.query.file.*;
import com.damdamdeo.pulse.extension.core.query.file.query.DownloadInput;
import com.damdamdeo.pulse.extension.core.query.file.query.DownloadQuery;
import com.damdamdeo.pulse.extension.core.query.file.query.GetFileInfoQuery;
import com.damdamdeo.pulse.extension.core.query.file.query.GetTraceByFileIdentifierQuery;
import com.damdamdeo.pulse.extension.core.query.file.traceability.DownloadedAt;
import com.damdamdeo.pulse.extension.core.query.file.traceability.DownloadedBy;
import com.damdamdeo.pulse.extension.core.query.file.traceability.Token;
import com.damdamdeo.pulse.extension.core.query.file.traceability.Traceability;
import io.quarkiverse.resteasy.problem.HttpProblem;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Path("file")
public class FileEndpoint {

    @Inject
    DownloadQuery downloadQuery;

    @Inject
    GetFileInfoQuery getFileInfoQuery;

    @Inject
    GetTraceByFileIdentifierQuery getTraceByFileIdentifierQuery;

    @GET
    @Path("{fileIdentifier}/download")
    @Operation(
            summary = "Download a file",
            description = "Download a file identified by his identifier."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "File downloaded successfully",
                    content = @Content(
                            mediaType = "*/*",
                            schema = @Schema(
                                    type = SchemaType.STRING,
                                    format = "binary"
                            )
                    ),
                    headers = {
                            @Header(
                                    name = "Content-Disposition",
                                    description = "Content disposition",
                                    schema = @Schema(type = SchemaType.STRING)
                            ),
                            @Header(
                                    name = "Content-Type",
                                    description = "File MIME Type.",
                                    schema = @Schema(type = SchemaType.STRING)
                            ),
                            @Header(
                                    name = "Content-Length",
                                    description = "File length.",
                                    schema = @Schema(
                                            type = SchemaType.INTEGER,
                                            format = "int64"
                                    )
                            )
                    }
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = HttpProblem.class)
                    )
            )
    })
    public Response download(
            @Parameter(
                    description = "File identifier",
                    required = true,
                    schema = @Schema(type = SchemaType.STRING)
            )
            @PathParam("fileIdentifier") final FileIdentifier fileIdentifier,
            @Parameter(
                    description = "Content disposition",
                    required = false,
                    schema = @Schema(
                            type = SchemaType.STRING,
                            enumeration = {"INLINE", "ATTACHMENT"},
                            defaultValue = "INLINE"
                    )
            )
            @QueryParam("contentDisposition") @DefaultValue("INLINE") final ContentDisposition contentDisposition) throws QueryException {
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

    @Schema(name = "FileInfo", required = true, requiredProperties = {"fileIdentifier",
            "filename", "contentType", "contentLength", "updatedAt", "uploadedBy", "ownedBy", "metadata"})
    public record FileInfoDTO(@Schema(type = SchemaType.STRING, implementation = String.class)
                              FileIdentifier fileIdentifier,
                              @Schema(type = SchemaType.STRING, implementation = String.class)
                              Filename filename,
                              ContentType contentType,
                              @Schema(type = SchemaType.INTEGER, implementation = Long.class)
                              ContentLength contentLength,
                              @Schema(type = SchemaType.STRING, implementation = String.class)
                              UploadedAt updatedAt,
                              @Schema(type = SchemaType.STRING, implementation = String.class)
                              UploadedBy uploadedBy,
                              @Schema(type = SchemaType.STRING, implementation = String.class)
                              OwnedBy ownedBy,
                              Map<String, List<String>> fileMetadata,
                              Map<String, String> customMetadata) {

        public FileInfoDTO {
            Objects.requireNonNull(fileIdentifier);
            Objects.requireNonNull(filename);
            Objects.requireNonNull(contentType);
            Objects.requireNonNull(contentLength);
            Objects.requireNonNull(updatedAt);
            Objects.requireNonNull(uploadedBy);
            Objects.requireNonNull(ownedBy);
            Objects.requireNonNull(fileMetadata);
            Objects.requireNonNull(customMetadata);
        }

        public static FileInfoDTO from(final FileInfo fileInfo) {
            Objects.requireNonNull(fileInfo);
            return new FileInfoDTO(
                    fileInfo.fileIdentifier(),
                    fileInfo.filename(),
                    fileInfo.contentType(),
                    fileInfo.contentLength(),
                    fileInfo.uploadedAt(),
                    fileInfo.uploadedBy(),
                    fileInfo.ownedBy(),
                    fileInfo.fileMetadata().metadata(),
                    fileInfo.customMetadata().metadata()
            );
        }
    }

    @Schema(name = "Traceability", required = true, requiredProperties = {"token", "fileIdentifier", "downloadedBy",
            "downloadedAt"})
    public record TraceabilityDTO(@Schema(type = SchemaType.STRING, implementation = String.class)
                                  Token token,
                                  @Schema(type = SchemaType.STRING, implementation = String.class)
                                  FileIdentifier fileIdentifier,
                                  @Schema(type = SchemaType.STRING, implementation = String.class)
                                  DownloadedBy downloadedBy,
                                  @Schema(type = SchemaType.STRING, implementation = String.class)
                                  DownloadedAt downloadedAt) {

        public TraceabilityDTO {
            Objects.requireNonNull(token);
            Objects.requireNonNull(fileIdentifier);
            Objects.requireNonNull(downloadedBy);
            Objects.requireNonNull(downloadedAt);
        }

        public static TraceabilityDTO from(final Traceability traceability) {
            Objects.requireNonNull(traceability);
            return new TraceabilityDTO(
                    traceability.token(),
                    traceability.fileIdentifier(),
                    traceability.downloadedBy(),
                    traceability.downloadedAt());
        }
    }

    @GET
    @Path("{fileIdentifier}/info")
    @Operation(
            summary = "Get file info",
            description = "Get file info by his identifier."
    )
    @APIResponses(
            value = {
                    @APIResponse(
                            responseCode = "200",
                            description = "File information retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = FileInfoDTO.class)
                            )
                    ),
                    @APIResponse(
                            responseCode = "500",
                            description = "Internal Server Error",
                            content = @Content(
                                    mediaType = "application/problem+json",
                                    schema = @Schema(implementation = HttpProblem.class)
                            )
                    )
            }
    )
    public FileInfoDTO getFileInfo(
            @Parameter(
                    description = "File identifier",
                    required = true,
                    schema = @Schema(type = SchemaType.STRING)
            )
            @PathParam("fileIdentifier") final FileIdentifier fileIdentifier) throws QueryException {
        return FileInfoDTO.from(getFileInfoQuery.execute(fileIdentifier));
    }

    @GET
    @Path("{fileIdentifier}/traceByFileIdentifier")
    @Operation(
            summary = "List of Traceability",
            description = "Return list of Traceability by his identifier."
    )
    @APIResponses(
            value = {
                    @APIResponse(
                            responseCode = "200",
                            description = "Traceability list retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            type = SchemaType.ARRAY,
                                            implementation = TraceabilityDTO.class
                                    )
                            )
                    ),
                    @APIResponse(
                            responseCode = "500",
                            description = "Internal Server Error",
                            content = @Content(
                                    mediaType = "application/problem+json",
                                    schema = @Schema(implementation = HttpProblem.class)
                            )
                    )
            }
    )
    public List<TraceabilityDTO> getTraceByFileIdentifier(
            @Parameter(
                    description = "File identifier",
                    required = true,
                    schema = @Schema(type = SchemaType.STRING)
            )
            @PathParam("fileIdentifier") final FileIdentifier fileIdentifier) throws QueryException {
        return getTraceByFileIdentifierQuery.execute(fileIdentifier).stream().map(TraceabilityDTO::from).toList();
    }
}
