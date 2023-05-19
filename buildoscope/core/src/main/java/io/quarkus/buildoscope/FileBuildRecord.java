package io.quarkus.buildoscope;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public interface FileBuildRecord {

    FileHash getFileHash();

    BuildActor getActor();

    Collection<FileHash> getDerivedFrom();

    FileStatus getStatus();

    default Mutable mutable() {
        return new FileBuildRecordImpl.Builder(this);
    }

    /**
     * Persist this file info to the specified file.
     *
     * @param p Target path
     * @throws IOException if the specified file can not be written to.
     */
    default void persist(Path p) throws IOException {
        BuildoscopeJsonMapper.serialize(this, p);
    }

    interface Mutable extends FileBuildRecord {

        Mutable setFileHash(FileHash fileHash);

        Mutable setActor(BuildActor actor);

        default Mutable setDerivedFrom(FileHash derivedFrom) {
            return setDerivedFrom(List.of(derivedFrom));
        }

        Mutable setDerivedFrom(Collection<FileHash> derivedFrom);

        Mutable setStatus(FileStatus status);

        FileBuildRecord build();

        default void persist(Path p) throws IOException {
            BuildoscopeJsonMapper.serialize(build(), p);
        }
    }

    /**
     * @return a new mutable instance
     */
    static Mutable builder() {
        return new FileBuildRecordImpl.Builder();
    }

    /**
     * Read from the specified file
     *
     * @param path File to read from (yaml or json)
     * @return read-only {@link FileBuildRecord} object
     * @throws IOException in case of a failure
     */
    static FileBuildRecord fromFile(Path path) throws IOException {
        return mutableFromFile(path).build();
    }

    /**
     * Read config from the specified file
     *
     * @param path File to read from (yaml or json)
     * @return mutable {@link FileBuildRecord}
     * @throws IOException in case of a failure
     */
    static FileBuildRecord.Mutable mutableFromFile(Path path) throws IOException {
        final FileBuildRecord.Mutable mutable = BuildoscopeJsonMapper.deserialize(path, FileBuildRecordImpl.Builder.class);
        return mutable == null ? FileBuildRecord.builder() : mutable;
    }

    /**
     * Read config from an input stream
     *
     * @param inputStream input stream to read from
     * @return read-only {@link FileBuildRecord} object (empty/default for an empty file)
     * @throws IOException in case of a failure
     */
    static FileBuildRecord fromStream(InputStream inputStream) throws IOException {
        final FileBuildRecord.Mutable mutable = BuildoscopeJsonMapper.deserialize(inputStream,
                FileBuildRecordImpl.Builder.class);
        return mutable == null ? FileBuildRecord.builder().build() : mutable.build();
    }
}
