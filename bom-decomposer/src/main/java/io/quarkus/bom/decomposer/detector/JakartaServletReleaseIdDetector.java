package io.quarkus.bom.decomposer.detector;

import io.quarkus.bom.decomposer.BomDecomposerException;
import io.quarkus.bom.decomposer.ReleaseId;
import io.quarkus.bom.decomposer.ReleaseIdDetector;
import io.quarkus.bom.decomposer.ReleaseIdFactory;
import io.quarkus.bom.decomposer.ReleaseIdResolver;
import org.eclipse.aether.artifact.Artifact;

public class JakartaServletReleaseIdDetector implements ReleaseIdDetector {

    @Override
    public ReleaseId detectReleaseId(ReleaseIdResolver releaseResolver, Artifact artifact)
            throws BomDecomposerException {
        if (artifact.getGroupId().equals("jakarta.servlet")) {
            ReleaseId releaseId = releaseResolver.defaultReleaseId(artifact);
            if (!releaseId.origin().toString().contains("eclipse-ee4j")) {
                return releaseId;
            }
            return ReleaseIdFactory.forScmAndTag("https://github.com/jakartaee/servlet", artifact.getVersion() + "-RELEASE");
        }
        return null;
    }

}
