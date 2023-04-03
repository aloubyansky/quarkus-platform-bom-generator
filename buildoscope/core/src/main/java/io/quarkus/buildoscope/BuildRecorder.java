package io.quarkus.buildoscope;

public interface BuildRecorder {

    static BuildRecorder newInstance() {
        return new BuildRecorderImpl();
    }

    void newBuild();

    void recordAction(BuildActor actor, FileSet stateBefore, FileSet stateAfter);

    void recordPublishing(FileHash file);

    void finalizeBuild();
}
