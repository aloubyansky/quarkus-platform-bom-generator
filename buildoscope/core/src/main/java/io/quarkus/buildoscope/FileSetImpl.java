package io.quarkus.buildoscope;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

public class FileSetImpl implements FileSet {

    public static FileSet of(Path dir, FileHasher hasher) {
        if (Files.isDirectory(dir)) {
            try (Stream<Path> s = Files.walk(dir)) {
                var result = new HashMap<String, FileHash>();
                var i = s.iterator();
                while (i.hasNext()) {
                    var p = i.next();
                    if (Files.isDirectory(p)) {
                        continue;
                    }
                    final String relativePath = FileHash.toPathString(dir.relativize(p));
                    result.put(relativePath, FileHash.of(relativePath, hasher.hash(p)));
                }
                return new FileSetImpl(result);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to walk directory " + dir, e);
            }
        }
        throw new IllegalArgumentException("Expected an existing directory but got " + dir);
    }

    private final Map<String, FileHash> files;

    protected FileSetImpl(Map<String, FileHash> files) {
        this.files = Collections.unmodifiableMap(files);
    }

    @Override
    public Iterator<FileHash> iterator() {
        return files.values().iterator();
    }

    @Override
    public boolean isEmpty() {
        return files.isEmpty();
    }

    @Override
    public Stream<FileHash> stream() {
        return files.values().stream();
    }

    @Override
    public FileHash getForPath(String path) {
        return files.get(path);
    }

    @Override
    public Map<String, FileHash> toMap() {
        return new HashMap<>(files);
    }
}
