package io.quarkus.domino.playground;

import io.quarkus.maven.dependency.ArtifactCoords;
import java.util.List;

public class PublishedModule {

    ArtifactCoords externalParent;
    List<ArtifactCoords> externalBomImports;
    List<ArtifactCoords> externalDependencyConstraints;
    List<ArtifactCoords> externalDirectDependencies;
}
