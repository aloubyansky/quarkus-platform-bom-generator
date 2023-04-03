package io.quarkus.buildoscope;

import java.util.Collection;

public interface BuildActionOutcome {

    BuildActor getActor();

    Collection<FileBuildRecord> getFiles();
}
