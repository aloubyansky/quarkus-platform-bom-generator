package io.quarkus.buildoscope;

import java.util.Objects;

public class FileHashImpl implements FileHash {

    private final String path;
    private final String hash;

    public FileHashImpl(String path, String hash) {
        this.path = Objects.requireNonNull(path);
        this.hash = Objects.requireNonNull(hash);
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getHash() {
        return hash;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash, path);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FileHashImpl other = (FileHashImpl) obj;
        return Objects.equals(hash, other.hash) && Objects.equals(path, other.path);
    }

    @Override
    public String toString() {
        return path + "#" + hash;
    }
}
