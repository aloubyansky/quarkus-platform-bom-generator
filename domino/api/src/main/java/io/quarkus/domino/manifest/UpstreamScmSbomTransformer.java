package io.quarkus.domino.manifest;

import io.quarkus.domino.RhVersionPattern;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Property;
import org.eclipse.aether.artifact.DefaultArtifact;

public class UpstreamScmSbomTransformer implements SbomTransformer {

    @Override
    public Bom transform(SbomTransformContext ctx) {

        System.out.println("UPSTREAM SCM SBOM TRANSFORMER " + ctx.getRevisionResolver());

        try (BufferedWriter writer = Files
                .newBufferedWriter(Path.of(UUID.randomUUID() + ".errors"))) {
            for (var c : ctx.getOriginalBom().getComponents()) {
                if (c.getPurl().startsWith("pkg:maven") && RhVersionPattern.isRhVersion(c.getVersion())) {
                    var a = new DefaultArtifact(c.getGroup(), c.getName(), "pom",
                            RhVersionPattern.ensureNoRhQualifier(c.getVersion()));
                    try {
                        var upstreamRevision = ctx.getRevisionResolver()
                                .resolveRevision(a, List.of());
                        var prop = new Property();
                        prop.setName("upstream-vcs");
                        prop.setValue(upstreamRevision.toString());
                        c.addProperty(prop);
                    } catch (Exception e) {
                        writer.write(a.toString());
                        writer.newLine();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ctx.getOriginalBom();
    }
}
