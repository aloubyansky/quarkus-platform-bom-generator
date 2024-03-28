package io.quarkus.domino;

import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import io.quarkus.maven.dependency.ArtifactCoords;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;

public class MavenProjectReader {

    private static final Map<String, String> PACKAGING_TYPE = Map.of(
            "maven-archetype", ArtifactCoords.TYPE_JAR,
            "bundle", ArtifactCoords.TYPE_JAR,
            "maven-plugin", ArtifactCoords.TYPE_JAR,
            "war", ArtifactCoords.TYPE_JAR);

    private static String getTypeForPackaging(String packaging) {
        return PACKAGING_TYPE.getOrDefault(packaging, packaging);
    }

    public static List<ArtifactCoords> resolveModuleDependencies(LocalWorkspace ws, Path currentDir) {
        Objects.requireNonNull(ws, "Workspace is null");
        var projectsByDir = new HashMap<Path, LocalProject>(ws.getProjects().size());
        LocalProject currentProject = null;
        for (var p : ws.getProjects().values()) {
            projectsByDir.put(p.getDir(), p);
            if (p.getDir().equals(currentDir)) {
                currentProject = p;
            }
        }
        if (currentProject == null) {
            throw new IllegalArgumentException("Failed to determine the current project at " + currentDir);
        }

        final List<ArtifactCoords> result = new ArrayList<>();
        final List<LocalProject> queued = new ArrayList<>(projectsByDir.size());
        queued.add(currentProject);
        for (int i = 0; i < queued.size(); ++i) {
            var project = queued.get(i);
            if (isPublished(project)) {
                result.add(ArtifactCoords.of(project.getGroupId(), project.getArtifactId(),
                        ArtifactCoords.DEFAULT_CLASSIFIER, getTypeForPackaging(project.getRawModel().getPackaging()),
                        project.getVersion()));
            }
            for (var module : getModules(project)) {
                var moduleDir = project.getDir().resolve(module).normalize().toAbsolutePath();
                var child = projectsByDir.get(moduleDir);
                if (child != null) {
                    queued.add(child);
                } else {
                    System.out.println("Failed to locate a project at " + moduleDir);
                }
            }
        }
        return result;
    }

    private static List<String> getModules(LocalProject project) {
        return getModel(project).getModules();
    }

    private static Model getModel(LocalProject currentProject) {
        return currentProject.getModelBuildingResult() == null ? currentProject.getRawModel()
                : currentProject.getModelBuildingResult().getEffectiveModel();
    }

    private static boolean isPublished(LocalProject project) {
        final Model model = getModel(project);
        var modelProps = model.getProperties();
        if (Boolean.parseBoolean(modelProps.getProperty("maven.install.skip"))
                || Boolean.parseBoolean(modelProps.getProperty("maven.deploy.skip"))
                || Boolean.parseBoolean(modelProps.getProperty("skipNexusStagingDeployMojo"))) {
            return false;
        }
        if (model.getBuild() != null) {
            for (Plugin plugin : model.getBuild().getPlugins()) {
                if (plugin.getArtifactId().equals("maven-install-plugin")
                        || plugin.getArtifactId().equals("maven-deploy-plugin")) {
                    for (PluginExecution e : plugin.getExecutions()) {
                        if (e.getId().startsWith("default-") && e.getPhase().equals("none")) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
}
