package io.quarkus.buildoscope;

import java.nio.file.Path;

public interface FileHasher {

    String hash(Path p);
}
