package io.quarkus.bom.decomposer.detector;

import io.quarkus.bom.decomposer.BomDecomposerException;
import io.quarkus.bom.decomposer.ReleaseIdDetector;
import io.quarkus.bom.decomposer.ReleaseIdResolver;
import io.quarkus.domino.scm.ScmRepository;
import io.quarkus.domino.scm.ScmRevision;
import org.eclipse.aether.artifact.Artifact;

public class JakartaCommonAnnotationsApiReleaseIdDetector implements ReleaseIdDetector {

    @Override
    public ScmRevision detectReleaseId(ReleaseIdResolver releaseResolver, Artifact artifact)
            throws BomDecomposerException {
        if (artifact.getGroupId().equals("jakarta.annotation")) {
            var releaseId = releaseResolver.defaultReleaseId(artifact);
            if (releaseId.getRepository().hasUrl()
                    && releaseId.getRepository().getUrl().startsWith("https://github.com/jakartaee/common-annotations-api")) {
                return releaseId;
            }
            return ScmRevision.tag(
                    ScmRepository.ofUrl("https://github.com/jakartaee/common-annotations-api"),
                    releaseId.getValue());
        }
        return null;
    }

}
