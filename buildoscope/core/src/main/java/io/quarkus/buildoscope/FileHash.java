package io.quarkus.buildoscope;

import java.nio.file.Path;
import java.util.StringJoiner;

public interface FileHash {

    static FileHash of(String path, String hash) {
        return new FileHashImpl(path, hash);
    }

    static String toPathString(Path relative) {
        final String relativeStr;
        if (relative.getNameCount() == 1) {
            relativeStr = relative.getFileName().toString();
        } else {
            var sb = new StringJoiner("/");
            for (Path e : relative) {
                sb.add(e.toString());
            }
            relativeStr = sb.toString();
        }
        return relativeStr;
    }

    String getPath();

    String getHash();
}
