package io.quarkus.domino.manifest;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.domino.ProjectDependencyConfig;
import io.quarkus.domino.ProjectDependencyResolver;
import io.quarkus.domino.TestUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.parsers.JsonParser;
import org.junit.jupiter.api.Test;

public class ProjectManifestingTest {

    @Test
    public void simplePom() {
        assertSbomMainComponent("projects/simple-pom", ArtifactCoords.pom("org.acme", "acme-parent", "1.0"));
    }

    @Test
    public void singleJar() {
        assertSbomMainComponent("projects/single-jar-and-bom", ArtifactCoords.jar("org.acme", "acme-library", "1.0"));
    }

    @Test
    public void projectBom() {
        final ArtifactCoords bom = ArtifactCoords.pom("org.acme", "acme-bom", "1.0");
        assertSbomMainComponent("projects/single-jar-and-bom", bom, config -> config.setProjectBom(bom));
    }

    private static void assertSbomMainComponent(String projectDirName, ArtifactCoords coords) {
        assertSbomMainComponent(projectDirName, coords, null);
    }

    private static void assertSbomMainComponent(String projectDirName, ArtifactCoords expectedCoords,
            Function<ProjectDependencyConfig.Mutable, ProjectDependencyConfig.Mutable> configurator) {
        var projectDir = TestUtils.getResource(projectDirName);
        var mainComponent = getMainComponent(getSbomForProjectDir(projectDir, configurator));

        assertThat(mainComponent).isNotNull();
        assertThat(mainComponent.getGroup()).isEqualTo(expectedCoords.getGroupId());
        assertThat(mainComponent.getName()).isEqualTo(expectedCoords.getArtifactId());
        assertThat(mainComponent.getVersion()).isEqualTo(expectedCoords.getVersion());
        assertThat(mainComponent.getPurl()).isEqualTo(PurgingDependencyTreeVisitor.getPurl(expectedCoords).toString());
        assertThat(mainComponent.getLicenses()).isNotNull();
        assertThat(mainComponent.getLicenses().getLicenses().size()).isEqualTo(1);
        var license = mainComponent.getLicenses().getLicenses().get(0);
        assertThat(license.getId()).isEqualTo("Apache-2.0");
        assertThat(mainComponent.getDescription()).isEqualTo("Description of " + mainComponent.getName());
        assertThat(mainComponent.getType()).isEqualTo(Component.Type.LIBRARY);
    }

    private static Bom getSbomForProjectDir(Path projectDir,
            Function<ProjectDependencyConfig.Mutable, ProjectDependencyConfig.Mutable> configurator) {
        Path output = null;
        try {
            output = Files.createTempFile("domino-test", "sbom");
            final MavenArtifactResolver artifactResolver = MavenArtifactResolver.builder()
                    .setOffline(true)
                    .setCurrentProject(projectDir.toString())
                    .build();
            var configBuilder = ProjectDependencyConfig.builder()
                    .setProjectDir(projectDir)
                    .setWarnOnMissingScm(true)
                    .setLegacyScmLocator(true);
            if (configurator != null) {
                configBuilder = configurator.apply(configBuilder);
            }
            var config = configBuilder.build();
            ProjectDependencyResolver.builder()
                    .setArtifactResolver(artifactResolver)
                    .setDependencyConfig(config)
                    .addDependencyTreeVisitor(new SbomGeneratingDependencyVisitor(
                            SbomGenerator.builder()
                                    .setArtifactResolver(artifactResolver)
                                    .setOutputFile(output)
                                    .setEnableTransformers(false)
                                    .setCalculateHashes(false),
                            config))
                    .build()
                    .resolveDependencies();

            try (BufferedReader reader = Files.newBufferedReader(output)) {
                return new JsonParser().parse(reader);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException | BootstrapMavenException e) {
            throw new RuntimeException(e);
        } finally {
            if (output != null) {
                output.toFile().deleteOnExit();
            }
        }
    }

    private static Component getMainComponent(Bom bom) {
        var metadata = bom.getMetadata();
        if (metadata == null) {
            return null;
        }
        return metadata.getComponent();
    }
}
