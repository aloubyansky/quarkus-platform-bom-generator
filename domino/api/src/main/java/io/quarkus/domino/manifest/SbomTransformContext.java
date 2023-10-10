package io.quarkus.domino.manifest;

import io.quarkus.bom.decomposer.ScmRevisionResolver;
import org.cyclonedx.model.Bom;

public interface SbomTransformContext {

    /**
     * The original BOM instance to be transformed
     * 
     * @return the original BOM instance to be transformed
     */
    Bom getOriginalBom();

    /**
     * SCM revision resolver
     *
     * @return SCM revision resolver
     */
    ScmRevisionResolver getRevisionResolver();
}
