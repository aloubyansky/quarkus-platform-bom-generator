package io.quarkus.domino.cli;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.options.BootstrapMavenOptions;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.util.GlobUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.JavaScopes;
import picocli.CommandLine;

abstract class BaseDependencyCommand implements Callable<Integer> {

    @CommandLine.Option(names = {
            "--bom" }, description = "Maven BOM dependency constraints of which should be processed as root artifacts.", required = false)
    protected String bom;

    @CommandLine.Option(names = {
            "--roots" }, description = "Maven artifacts whose dependencies should be processed", required = false, split = ",")
    protected List<String> roots = List.of();

    @CommandLine.Option(names = { "--project-dir" }, description = "Local repository directory")
    public String projectDir;

    @CommandLine.Option(names = {
            "--versions" }, description = "Limit root artifact versions to those matching specified glob patterns", split = ",")
    protected List<String> versions = List.of();

    @CommandLine.Option(names = {
            "--settings",
            "-s" }, description = "A path to Maven settings that should be used when initializing the Maven resolver")
    protected String settings;

    @CommandLine.Option(names = {
            "--maven-profiles",
            "-P" }, description = "Comma-separated list of Maven profiles that should be enabled when resolving dependencies")
    public String mavenProfiles;

    @CommandLine.Option(names = { "--repo-dir" }, description = "Local repository directory")
    public String repoDir;

    private final MessageWriter log = MessageWriter.info();

    private MavenArtifactResolver resolver;

    protected MessageWriter log() {
        return log;
    }

    protected List<Dependency> getRoots() {

        final List<Pattern> versionPatterns;
        if (versions.isEmpty()) {
            versionPatterns = List.of();
        } else {
            var patterns = new ArrayList<Pattern>(versions.size());
            for (var vp : versions) {
                patterns.add(Pattern.compile(GlobUtil.toRegexPattern(vp)));
            }
            versionPatterns = patterns;
        }

        List<Dependency> unfiltered;
        if (!roots.isEmpty()) {
            unfiltered = new ArrayList<>(roots.size());
            for (var root : roots) {
                var coords = ArtifactCoords.fromString(root);
                unfiltered.add(new Dependency(
                        new DefaultArtifact(coords.getGroupId(), coords.getArtifactId(), coords.getClassifier(),
                                coords.getType(), coords.getVersion()),
                        JavaScopes.COMPILE));
            }
        } else if (bom != null) {
            var coords = ArtifactCoords.fromString(bom);
            var bomArtifact = new DefaultArtifact(coords.getGroupId(), coords.getArtifactId(), ArtifactCoords.TYPE_POM,
                    coords.getVersion());
            try {
                unfiltered = getResolver().resolveDescriptor(bomArtifact).getManagedDependencies();
            } catch (BootstrapMavenException e) {
                throw new RuntimeException("Failed to resolve descriptor of " + coords.toCompactCoords(), e);
            }
            if (unfiltered.isEmpty()) {
                throw new RuntimeException(
                        coords.toCompactCoords() + " either does not include dependency management or could not be resolved");
            }
        } else {
            var workspace = getResolver().getMavenContext().getWorkspace();
            if (workspace == null) {
                throw new RuntimeException("Project workspace is not available");
            }
            unfiltered = new ArrayList<>(workspace.getProjects().size());
            for (var project : workspace.getProjects().values()) {
                unfiltered.add(new Dependency(
                        new DefaultArtifact(project.getGroupId(), project.getArtifactId(), ArtifactCoords.TYPE_POM,
                                project.getVersion()),
                        ArtifactCoords.TYPE_POM));
            }
        }
        if (versionPatterns.isEmpty()) {
            return unfiltered;
        }
        var result = new ArrayList<Dependency>(unfiltered.size());
        for (var d : unfiltered) {
            if (isVersionIncluded(d.getArtifact().getVersion(), versionPatterns)) {
                result.add(d);
            }
        }
        return result;
    }

    private static boolean isVersionIncluded(String version, List<Pattern> patterns) {
        for (var pattern : patterns) {
            if (pattern.matcher(version).matches()) {
                return true;
            }
        }
        return false;
    }

    protected MavenArtifactResolver getResolver() {
        if (resolver != null) {
            return resolver;
        }
        var config = BootstrapMavenContext.config()
                .setArtifactTransferLogging(false);
        if (settings != null) {
            var f = new File(settings);
            if (!f.exists()) {
                throw new IllegalArgumentException(f + " does not exist");
            }
            config.setUserSettings(f);
        }
        if (repoDir != null) {
            config.setLocalRepository(repoDir);
        }
        if (mavenProfiles != null) {
            System.setProperty(BootstrapMavenOptions.QUARKUS_INTERNAL_MAVEN_CMD_LINE_ARGS, "-P" + mavenProfiles);
        }
        if (projectDir == null && roots.isEmpty() && bom == null) {
            projectDir = "";
        }
        if (projectDir == null) {
            config.setWorkspaceDiscovery(false);
        } else {
            config.setWorkspaceDiscovery(true)
                    .setCurrentProject(projectDir)
                    .setPreferPomsFromWorkspace(true)
                    .setEffectiveModelBuilder(true);
        }
        try {
            return resolver = new MavenArtifactResolver(new BootstrapMavenContext(config));
        } catch (BootstrapMavenException e) {
            throw new RuntimeException("Failed to initialize Maven artifact resolver", e);
        }
    }
}
