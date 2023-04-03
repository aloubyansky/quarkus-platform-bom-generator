package io.quarkus.buildoscope;

import java.util.ArrayList;
import java.util.Map;

public abstract class BaseBuildActionRecorder implements BuildActionRecorder {

    @Override
    public BuildActionOutcome record(Context ctx) {
        var records = new ArrayList<FileBuildRecord>();
        var mapBefore = ctx.getStateBefore().toMap();
        var mapAfter = ctx.getStateAfter().toMap();
        for (Map.Entry<String, FileHash> fileBefore : mapBefore.entrySet()) {
            var fileAfter = mapAfter.remove(fileBefore.getKey());
            if (fileAfter == null) {
                var fileRecord = FileBuildRecord.builder()
                        .setActor(ctx.getActor())
                        .setStatus(FileBuildStatus.REMOVED)
                        .setFileHash(fileBefore.getValue())
                        .build();
                records.add(fileRecord);
            } else if (!fileBefore.getValue().getHash().equals(fileAfter.getHash())) {
                var fileRecord = FileBuildRecord.builder()
                        .setActor(ctx.getActor())
                        .setStatus(FileBuildStatus.MODIFIED)
                        .setFileHash(fileAfter);
                setDerivedFrom(fileRecord);
                records.add(fileRecord.build());
            }
        }
        for (FileHash created : mapAfter.values()) {
            var fileRecord = FileBuildRecord.builder()
                    .setActor(ctx.getActor())
                    .setStatus(FileBuildStatus.CREATED)
                    .setFileHash(created);
            setDerivedFrom(fileRecord);
            records.add(fileRecord.build());
        }
        return BuildActionOutcomeImpl.of(ctx.getActor(), records);
    }

    protected abstract void setDerivedFrom(FileBuildRecord.Mutable fileRecord);
}
