package io.quarkus.platform;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.impl.Deployer;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.AuthenticationDigest;
import org.eclipse.aether.repository.RemoteRepository;

@Mojo(name = "deploy")
public class DeployerMojo extends AbstractMojo {

    @Component
    RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project}")
    MavenProject project;

    @Component
    Deployer deployer;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        System.out.println("hello");

        var distRepo = project.getDistributionManagementArtifactRepository();
        final RemoteRepository repo = new RemoteRepository.Builder(distRepo.getId(), "default", distRepo.getUrl())
                .setAuthentication(new Authentication() {
                    @Override
                    public void fill(AuthenticationContext context, String key, Map<String, String> data) {
                        System.out.println("fill " + key + " " + data);
                        if ("username".equals(key)) {
                            context.put(key, "aloubyansky");
                        } else if ("password".equals(key)) {
                            context.put(key, "La#Ca55arde!");
                        }
                    }

                    @Override
                    public void digest(AuthenticationDigest digest) {
                        System.out.println("digest ");
                    }
                })
                .build();
        System.out.println("REPO " + repo);

        Artifact a = new DefaultArtifact("io.quarkus.platform", "quarkus-bom", "sbom", "json", "999-SNAPSHOT", Map.of(),
                new DefaultArtifactType("io.quarkus.platform:quarkus-bom:sbom:json:999-SNAPSHOT",
                        "json", "sbom", "none", false, false));
        a = a.setFile(new File("/home/aloubyansky/git/quarkus-platform/target/sbom/quarkus-bom-sbom.json"));

        org.apache.maven.artifact.repository.metadata.Metadata repoMd = new org.apache.maven.artifact.repository.metadata.Metadata();
        repoMd.setGroupId("io.quarkus.platform");
        repoMd.setArtifactId("quarkus-bom");
        repoMd.setVersion("999-SNAPSHOT");
        repoMd.setModelVersion("1.1.0");
        Versioning versioning = new Versioning();
        repoMd.setVersioning(versioning);
        Date date = new Date();
        versioning.setLastUpdatedTimestamp(date);
        SnapshotVersion sn = new SnapshotVersion();
        sn.setClassifier("sbom");
        sn.setExtension("json");
        sn.setVersion("999-SNAPSHOT");
        sn.setUpdated(String.valueOf(date.getTime()));
        versioning.setSnapshotVersions(List.of(
                sn));

        Path mdXml = Path.of("metadata.xml");
        try (BufferedWriter writer = Files.newBufferedWriter(mdXml)) {
            new MetadataXpp3Writer().write(writer, repoMd);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final DefaultMetadata md = new DefaultMetadata(
                "io.quarkus.platform", "quarkus-bom", "999-SNAPSHOT", "json", Metadata.Nature.SNAPSHOT, Map.of(),
                mdXml.toFile());
        try {
            deployer.deploy(repoSession, new DeployRequest()
                    .setRepository(repo)
                    .setMetadata(List.of(md))
                    .setArtifacts(List.of(a)));
        } catch (DeploymentException e) {
            throw new MojoExecutionException(e);
        }

    }
}
