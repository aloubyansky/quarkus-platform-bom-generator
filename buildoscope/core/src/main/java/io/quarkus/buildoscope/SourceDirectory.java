package io.quarkus.buildoscope;

import java.nio.file.Path;
import java.util.Objects;

public class SourceDirectory implements BuildActor {

    public static final String KIND = "Source directory";

    public static SourceDirectory of(Path p) {
        return new SourceDirectory(p.normalize().toAbsolutePath().toString());
    }

    public static SourceDirectory of(String s) {
        return new SourceDirectory(s);
    }

    private final String directory;

    public SourceDirectory(String directory) {
        this.directory = directory;
    }

    @Override
    public String getId() {
        return directory;
    }

    @Override
    public String getKind() {
        return KIND;
    }

    @Override
    public String toString() {
        return directory;
    }

    @Override
    public int hashCode() {
        return Objects.hash(directory);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SourceDirectory other = (SourceDirectory) obj;
        return Objects.equals(directory, other.directory);
    }
}
