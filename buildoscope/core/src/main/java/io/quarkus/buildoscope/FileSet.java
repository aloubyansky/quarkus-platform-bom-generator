package io.quarkus.buildoscope;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

public interface FileSet extends Iterable<FileHash> {

    public static FileSet empty() {
        return new FileSetImpl(Map.of());
    }

    public static FileSet of(Path dir, FileHasher hasher) {
        return FileSetImpl.of(dir, hasher);
    }

    boolean isEmpty();

    Stream<FileHash> stream();

    FileHash getForPath(String path);

    Map<String, FileHash> toMap();
}
