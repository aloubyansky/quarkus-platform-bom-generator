package io.quarkus.buildoscope;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class FileBuildRecordImpl implements FileBuildRecord {

    private final FileHash fileHash;
    private final BuildActor actor;
    private final Collection<FileHash> derivedFrom;
    private final FileBuildStatus status;

    public FileBuildRecordImpl(FileBuildRecord other) {
        fileHash = other.getFileHash();
        actor = other.getActor();
        derivedFrom = other.getDerivedFrom();
        status = other.getStatus();
    }

    @Override
    public FileHash getFileHash() {
        return fileHash;
    }

    @Override
    public BuildActor getActor() {
        return actor;
    }

    @Override
    public Collection<FileHash> getDerivedFrom() {
        return derivedFrom;
    }

    @Override
    public FileBuildStatus getStatus() {
        return status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(derivedFrom, fileHash, actor, status);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FileBuildRecordImpl other = (FileBuildRecordImpl) obj;
        return Objects.equals(derivedFrom, other.derivedFrom) && Objects.equals(fileHash, other.fileHash)
                && Objects.equals(actor, other.actor) && Objects.equals(status, other.status);
    }

    @Override
    public String toString() {
        return FileBuildRecordImpl.toString(this);
    }

    static class Builder implements FileBuildRecord.Mutable {

        private FileHash fileHash;
        private BuildActor actor;
        private Collection<FileHash> derivedFrom = List.of();
        private FileBuildStatus status;

        Builder() {
        }

        Builder(FileBuildRecord other) {
            fileHash = other.getFileHash();
            actor = other.getActor();
            derivedFrom = other.getDerivedFrom();
            status = other.getStatus();
        }

        @Override
        public FileHash getFileHash() {
            return fileHash;
        }

        @Override
        public BuildActor getActor() {
            return actor;
        }

        @Override
        public Collection<FileHash> getDerivedFrom() {
            return derivedFrom;
        }

        @Override
        public FileBuildStatus getStatus() {
            return status;
        }

        @Override
        public Mutable setFileHash(FileHash fileHash) {
            this.fileHash = fileHash;
            return this;
        }

        @Override
        public Mutable setActor(BuildActor actor) {
            this.actor = actor;
            return this;
        }

        @Override
        public Mutable setDerivedFrom(Collection<FileHash> derivedFrom) {
            this.derivedFrom = derivedFrom;
            return this;
        }

        @Override
        public Mutable setStatus(FileBuildStatus status) {
            this.status = status;
            return this;
        }

        @Override
        public FileBuildRecord build() {
            return new FileBuildRecordImpl(this);
        }

        @Override
        public int hashCode() {
            return Objects.hash(derivedFrom, fileHash, actor);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Builder other = (Builder) obj;
            return Objects.equals(derivedFrom, other.derivedFrom) && Objects.equals(fileHash, other.fileHash)
                    && Objects.equals(actor, other.actor);
        }

        @Override
        public String toString() {
            return FileBuildRecordImpl.toString(this);
        }
    }

    private static String toString(FileBuildRecord f) {
        return f.getFileHash() + "@" + f.getActor();
    }
}
