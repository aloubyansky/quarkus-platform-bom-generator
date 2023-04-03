package io.quarkus.buildoscope.maven;

import io.quarkus.buildoscope.BuildActor;
import io.quarkus.buildoscope.BuildRecorder;
import io.quarkus.buildoscope.FileDigestHasher;
import io.quarkus.buildoscope.FileHash;
import io.quarkus.buildoscope.FileHasher;
import io.quarkus.buildoscope.FileSet;
import io.quarkus.buildoscope.MavenMojoExecution;
import io.quarkus.buildoscope.MavenMojoExecution.MojoKey;
import io.quarkus.buildoscope.SourceDirectory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.project.MavenProject;

@Named("buildoscope-maven-spy")
@Singleton
public class BuildoscopeMavenSpy extends AbstractEventSpy {

    private static final FileHasher fileHasher = FileDigestHasher.sha256();

    private BuildRecorder buildRecorder;
    private final Map<String, Map<BuildActor, FileSet>> statesBefore = new ConcurrentHashMap<>();

    @Override
    public void init(Context context) throws Exception {
        buildRecorder = BuildRecorder.newInstance();
        buildRecorder.newBuild();
    }

    @Override
    public void onEvent(Object event) throws Exception {
        if (!(event instanceof ExecutionEvent)) {
            return;
        }
        ExecutionEvent e = (ExecutionEvent) event;
        switch (e.getType()) {
            case MojoStarted: {
                var actor = getActor(e);
                statesBefore.put(e.getProject().getId(), Map.of(actor, getProjectFileSet(e)));
                break;
            }
            case MojoSucceeded:
            case MojoFailed: {
                var projectStatesBefore = statesBefore.get(e.getProject().getId());
                if (projectStatesBefore != null) {
                    var actor = getActor(e);
                    var stateBefore = projectStatesBefore.get(actor);
                    if (stateBefore != null) {
                        buildRecorder.recordAction(actor, stateBefore, getProjectFileSet(e));
                        if (actor.matchesKey(MojoKey.INSTALL)) {
                            var main = e.getProject().getArtifact();
                            if (!main.getType().equals("pom")) {
                                buildRecorder.recordPublishing(
                                        getFileHash(e.getProject(), e.getProject().getModel().getPomFile().toPath()));
                            }
                            buildRecorder.recordPublishing(getFileHash(e.getProject(), main.getFile().toPath()));
                            for (var a : e.getProject().getAttachedArtifacts()) {
                                buildRecorder.recordPublishing(getFileHash(e.getProject(), a.getFile().toPath()));
                            }
                        } else if (actor.matchesKey(MojoKey.COMPILE)) {
                            //System.out.println("COMPILER SRC ROOTS: "
                            //        + e.getMojoExecution().getConfiguration().getChild("compileSourceRoots"));
                            //var classesDir = Path.of(e.getProject().getBuild().getOutputDirectory());
                            //if(Files.isDirectory(classesDir)) {
                            //}
                        }
                    } else {
                        System.out.println(
                                "[ERROR] [" + getClass().getSimpleName() + "] " + actor
                                        + " is missing the before file set state");
                    }
                }
                break;
            }
            case ProjectStarted: {
                for (String s : e.getProject().getCompileSourceRoots()) {
                    var dir = Path.of(s);
                    if (Files.isDirectory(dir)) {
                        buildRecorder.recordAction(SourceDirectory.of(dir), FileSet.empty(), FileSet.of(dir, fileHasher));
                    }
                }
                for (String s : e.getProject().getTestCompileSourceRoots()) {
                    var dir = Path.of(s);
                    if (Files.isDirectory(dir)) {
                        buildRecorder.recordAction(SourceDirectory.of(dir), FileSet.empty(), FileSet.of(dir, fileHasher));
                    }
                }
            }
            case SessionEnded: {
                buildRecorder.finalizeBuild();
                break;
            }
            default:
        }
    }

    private static FileHash getFileHash(MavenProject project, Path p) {
        var baseDir = project.getBasedir().toPath();
        return FileHash.of(FileHash.toPathString(baseDir.relativize(p)), fileHasher.hash(p));
    }

    private static FileSet getProjectFileSet(ExecutionEvent e) {
        return FileSet.of(e.getProject().getBasedir().toPath(), fileHasher);
    }

    private static MavenMojoExecution getActor(ExecutionEvent e) {
        var exec = e.getMojoExecution();
        return MavenMojoExecution.of(exec.getGroupId(), exec.getArtifactId(), exec.getVersion(), exec.getGoal(),
                exec.getExecutionId());
    }

    @Override
    public void close() throws Exception {
    }
}
