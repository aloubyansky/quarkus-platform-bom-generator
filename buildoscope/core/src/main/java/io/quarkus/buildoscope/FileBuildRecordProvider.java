package io.quarkus.buildoscope;

public interface FileBuildRecordProvider {

    FileBuildRecord getBuildRecord(FileHash fileHash);
}
