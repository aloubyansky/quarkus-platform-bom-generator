package io.quarkus.buildoscope;

public interface BuildActionRecorder {

    interface Context {

        BuildActor getActor();

        FileSet getStateBefore();

        FileSet getStateAfter();
    }

    BuildActionOutcome record(Context ctx);
}
