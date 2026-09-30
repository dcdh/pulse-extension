package com.damdamdeo.pulse.extension.common.runtime.featureflag;

import com.damdamdeo.pulse.extension.core.featureflag.Feature;
import com.damdamdeo.pulse.extension.core.featureflag.FeaturesProvider;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import java.util.List;

@Path("/features")
@ApplicationScoped
@Unremovable
public class FeatureEndpoint {

    @Inject
    FeaturesProvider featuresProvider;

    @GET
    @Operation(
            summary = "Enabled features",
            description = "Returns enabled features."
    )
    @APIResponses(
            value = {
                    @APIResponse(
                            responseCode = "200",
                            description = "Enabled features list retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            type = SchemaType.ARRAY,
                                            implementation = String.class,
                                            examples = {
                                                    "[\"TRACEABILITY\"]"
                                            }
                                    )
                            )
                    )
            }
    )
    public List<String> enabledFeature() {
        return featuresProvider.provideAll().stream().filter(Feature::isEnabled).map(Feature::name).toList();
    }
}
