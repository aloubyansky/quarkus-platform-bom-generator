package io.quarkus.domino.manifest;

import org.cyclonedx.model.Bom;

public class UpstreamScmSbomTransformer implements SbomTransformer {

    @Override
    public Bom transform(SbomTransformContext ctx) {

        System.out.println("UPSTREAM SCM SBOM TRANSFORMER " + ctx.getRevisionResolver());
        return null;
    }
}
