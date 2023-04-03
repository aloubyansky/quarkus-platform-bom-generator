package io.quarkus.buildoscope;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FileSetOfDirectoryTest {

    @TempDir
    Path workDir;

    @Test
    public void testEmptyFileSetDirectory() {
        final FileSet fs = fileSetOf(workDir);
        assertThat(fs).isEmpty();
    }

    @Test
    public void testFileSetDirectory() {

        String oneTxt = "one.txt";
        String twoTxt = "one/two.txt";
        String threeTxt = "one/two/three.txt";
        createFile(oneTxt, "one");
        createFile(twoTxt, "two");
        createFile(threeTxt, "three");

        final FileSet fs = fileSetOf(workDir);
        assertThat(fs.stream().map(FileHash::getPath).collect(Collectors.toSet()))
                .isEqualTo(Set.of(oneTxt, twoTxt, threeTxt));

        assertThat(fs.getForPath(oneTxt)).extracting(FileHash::getPath).isEqualTo(oneTxt);
        assertThat(fs.getForPath(twoTxt)).extracting(FileHash::getPath).isEqualTo(twoTxt);
        assertThat(fs.getForPath(threeTxt)).extracting(FileHash::getPath).isEqualTo(threeTxt);
    }

    private void createFile(String path, String content) {
        final Path target = workDir.resolve(path);
        try {
            Files.createDirectories(target.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(target)) {
                writer.write(content);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create file " + path, e);
        }
    }

    private static FileSet fileSetOf(Path dir) {
        return FileSet.of(dir, FileDigestHasher.sha256());
    }
}
