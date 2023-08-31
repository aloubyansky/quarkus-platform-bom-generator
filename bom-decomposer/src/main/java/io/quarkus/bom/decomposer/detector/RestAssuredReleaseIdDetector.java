package io.quarkus.bom.decomposer.detector;

import io.quarkus.bom.decomposer.BomDecomposerException;
import io.quarkus.bom.decomposer.ReleaseIdDetector;
import io.quarkus.bom.decomposer.ReleaseIdResolver;
import io.quarkus.domino.scm.ScmRevision;
import org.eclipse.aether.artifact.Artifact;

public class RestAssuredReleaseIdDetector implements ReleaseIdDetector {

    @Override
    public ScmRevision detectReleaseId(ReleaseIdResolver releaseResolver, Artifact artifact)
            throws BomDecomposerException {
        if (artifact.getGroupId().equals("io.rest-assured")) {
            var releaseId = releaseResolver.defaultReleaseId(artifact);
            String version = releaseId.getValue();
            if (version.startsWith("rest-assured-")) {
                return releaseId;
            }
            return ScmRevision.tag(releaseId.getRepository(), "rest-assured-" + version);
        }
        return null;
    }
}
