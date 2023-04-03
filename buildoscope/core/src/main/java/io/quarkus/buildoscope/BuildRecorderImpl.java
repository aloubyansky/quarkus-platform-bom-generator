package io.quarkus.buildoscope;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

class BuildRecorderImpl implements BuildRecorder {

    private static class RecorderContext implements BuildActionRecorder.Context {
        final BuildActor actor;
        final FileSet stateBefore;
        final FileSet stateAfter;

        public RecorderContext(BuildActor actor, FileSet stateBefore, FileSet stateAfter) {
            this.actor = actor;
            this.stateBefore = stateBefore;
            this.stateAfter = stateAfter;
        }

        @Override
        public BuildActor getActor() {
            return actor;
        }

        @Override
        public FileSet getStateBefore() {
            return stateBefore;
        }

        @Override
        public FileSet getStateAfter() {
            return stateAfter;
        }
    }

    private final BuildActionRecorder actionRecorder = new BaseBuildActionRecorder() {
        @Override
        protected void setDerivedFrom(FileBuildRecord.Mutable fileRecord) {
            var path = fileRecord.getFileHash().getPath();
            if (path.endsWith(".class")) {
                var dollar = path.indexOf('$');
                if (dollar > 0) {
                    path = path.substring(0, dollar) + ".java";
                } else {
                    path = path.substring(0, path.length() - ".class".length()) + ".java";
                }
                var lastHash = lastFileHash.get(path);
                if (lastHash == null) {
                    log("[ERROR] failed to locate Java source " + path + " for " + fileRecord.getFileHash().getPath());
                } else {
                    fileRecord.setDerivedFrom(List.of(lastHash));
                    log(fileRecord.getFileHash().getPath() + " is derived from " + lastHash.getPath());
                }
            } else if (path.endsWith(".jar")) {

            }
        }
    };

    private final Map<String, FileHash> lastFileHash = new ConcurrentHashMap<>();
    private final Map<FileHash, FileBuildRecord> allFileHashes = new ConcurrentHashMap<>();
    private final Map<BuildActor, ConcurrentLinkedQueue<FileBuildRecord>> buildActors = new ConcurrentHashMap<>();

    BuildRecorderImpl() {
    }

    @Override
    public void newBuild() {
        log("new build");
    }

    @Override
    public void recordAction(BuildActor actor, FileSet stateBefore, FileSet stateAfter) {
        log(actor);
        var actionRecorder = getRecorderFor(actor);
        if (actionRecorder != null) {
            var actorRecords = buildActors.computeIfAbsent(actor, k -> new ConcurrentLinkedQueue<>());
            var record = actionRecorder.record(new RecorderContext(actor, stateBefore, stateAfter));
            log("build actor " + record.getActor());
            for (FileBuildRecord f : record.getFiles()) {
                allFileHashes.put(f.getFileHash(), f);
                actorRecords.add(f);
                lastFileHash.put(f.getFileHash().getPath(), f.getFileHash());
                log(" " + f.getStatus().toString().toLowerCase() + " " + f.getFileHash().getPath());
            }
        }
    }

    @Override
    public void recordPublishing(FileHash file) {
        log("published " + file.getPath());
    }

    private BuildActionRecorder getRecorderFor(BuildActor actor) {
        return actionRecorder;
    }

    @Override
    public void finalizeBuild() {
        log("build terminated");
    }

    private static void log(Object msg) {
        System.out.println("Buildoscope: " + String.valueOf(msg));
    }
}
