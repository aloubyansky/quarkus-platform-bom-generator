package io.quarkus.buildoscope;

import java.util.Collection;
import java.util.Objects;

class BuildActionOutcomeImpl implements BuildActionOutcome {

    static BuildActionOutcome of(BuildActor actor, Collection<FileBuildRecord> files) {
        return new BuildActionOutcomeImpl(actor, files);
    }

    private final BuildActor actor;
    private final Collection<FileBuildRecord> files;

    private BuildActionOutcomeImpl(BuildActor actor, Collection<FileBuildRecord> files) {
        this.actor = Objects.requireNonNull(actor);
        this.files = Objects.requireNonNull(files);
    }

    @Override
    public BuildActor getActor() {
        return actor;
    }

    @Override
    public Collection<FileBuildRecord> getFiles() {
        return files;
    }
}
