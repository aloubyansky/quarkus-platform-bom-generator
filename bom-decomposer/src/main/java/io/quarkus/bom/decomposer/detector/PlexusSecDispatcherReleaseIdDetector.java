package io.quarkus.bom.decomposer.detector;

import io.quarkus.bom.decomposer.BomDecomposerException;
import io.quarkus.bom.decomposer.ReleaseIdDetector;
import io.quarkus.bom.decomposer.ReleaseIdResolver;
import io.quarkus.domino.scm.ScmRepository;
import io.quarkus.domino.scm.ScmRevision;
import org.eclipse.aether.artifact.Artifact;

public class PlexusSecDispatcherReleaseIdDetector implements ReleaseIdDetector {

    @Override
    public ScmRevision detectReleaseId(ReleaseIdResolver releaseResolver, Artifact artifact)
            throws BomDecomposerException {
        if (artifact.getGroupId().equals("org.sonatype.plexus")
                && artifact.getArtifactId().equals("plexus-sec-dispatcher")) {
            var releaseId = releaseResolver.defaultReleaseId(artifact);
            if (releaseId.getRepository().hasUrl()
                    && releaseId.getRepository().getUrl().equals("https://github.com/codehaus-plexus/plexus-sec-dispatcher")) {
                return releaseId;
            }
            return ScmRevision.tag(
                    ScmRepository.ofUrl("https://github.com/codehaus-plexus/plexus-sec-dispatcher"),
                    "sec-dispatcher-" + releaseId.getValue());
        }
        return null;
    }

}
