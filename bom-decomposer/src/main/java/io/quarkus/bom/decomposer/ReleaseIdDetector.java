package io.quarkus.bom.decomposer;

import io.quarkus.domino.scm.ScmRevision;
import org.eclipse.aether.artifact.Artifact;

public interface ReleaseIdDetector {

    ScmRevision detectReleaseId(ScmRevisionResolver releaseResolver, Artifact artifact) throws BomDecomposerException;
}
