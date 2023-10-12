package io.quarkus.domino.cli;

import com.google.common.io.ByteStreams;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.paths.PathTree;
import io.quarkus.paths.PathVisit;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.maven.model.Model;
import org.eclipse.jgit.util.Hex;
import picocli.CommandLine;

@CommandLine.Command(name = "dist")
public class Dist implements Callable<Integer> {

    private static final String ALG = "SHA-512";
    private static final Set<String> CHECKSUM_SUFFIXES = Set.of("sha1", "md5", "asc");

    @CommandLine.Option(names = { "--zip" }, description = "Distribution to analyze", required = true)
    public Path dist;

    @CommandLine.Option(names = { "--repo" }, description = "Maven repository", required = true)
    public Path repo;

    private MessageDigest digest;

    @Override
    public Integer call() throws Exception {

        if (!Files.exists(dist)) {
            throw new IllegalArgumentException(dist + " does not exist");
        }
        if (!Files.exists(repo)) {
            throw new IllegalArgumentException(repo + " does not exist");
        }

        try {
            digest = MessageDigest.getInstance(ALG);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        int totalArchives = 0;
        int matchedArtifacts = 0;
        int mavenMetadata = 0;
        final Map<String, ArtifactCoords> indexedRepo = indexRepo(repo);
        try (var zipIs = new ZipInputStream(Files.newInputStream(dist))) {
            var entry = zipIs.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    if (entry.getName().endsWith(".jar") || entry.getName().endsWith(".zip")) {
                        ++totalArchives;
                        final byte[] entryBytes = ByteStreams.toByteArray(zipIs);
                        var hash = hash(entryBytes);
                        var coords = indexedRepo.get(hash);
                        if (coords != null) {
                            matchedArtifacts++;
                        } else {
                            try (var nestedArchive = new ZipInputStream(new ByteArrayInputStream(entryBytes))) {
                                ZipEntry e;
                                boolean hasMavenInfo = false;
                                while ((e = nestedArchive.getNextEntry()) != null) {
                                    if (e.getName().startsWith("META-INF/maven/") && e.getName().endsWith("pom.properties")) {
                                        mavenMetadata++;
                                        hasMavenInfo = true;
                                        break;
                                    }
                                }
                                if (!hasMavenInfo) {
                                    System.out.println("Unknown archive: " + entry.getName());
                                }
                            }
                        }
                    } else {
                        //System.out.println(entry.getName());
                    }
                }
                entry = zipIs.getNextEntry();
            }
        }

        System.out.println("Total archives: " + totalArchives);
        System.out.println("Matched artifacts: " + matchedArtifacts);
        System.out.println("Has Maven metadata: " + mavenMetadata);
        System.out.println("Remaining archives: " + (totalArchives - matchedArtifacts - mavenMetadata));
        return CommandLine.ExitCode.OK;
    }

    private String hash(byte[] bytes) {
        return Hex.toHexString(digest.digest(bytes));
    }

    private Map<String, ArtifactCoords> indexRepo(Path repo) {
        final Map<String, ArtifactCoords> result = new HashMap<>();
        try (var tree = PathTree.ofDirectoryOrArchive(repo).open()) {
            // find the repo dir
            final AtomicReference<String> repoDir = new AtomicReference<>();
            tree.walk(visit -> {
                var fn = visit.getPath().getFileName();
                if (fn != null && fn.toString().endsWith(".pom")) {
                    visit.stopWalking();
                    final Model model;
                    try {
                        model = ModelUtils.readModel(visit.getPath());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    var artifactPath = ModelUtils.getGroupId(model).replace(".", "/") + "/" + model.getArtifactId();
                    var relativePath = visit.getRelativePath("/");
                    var i = relativePath.indexOf(artifactPath);
                    repoDir.set(relativePath.substring(0, i));
                }
            });
            if (repoDir.get() == null) {
                throw new IllegalArgumentException("Failed to determine the repository directory in " + repo);
            }
            tree.accept(repoDir.get(), repoVisit -> {
                PathTree.ofDirectoryOrArchive(repoVisit.getPath()).walk(visit -> {
                    if (Files.isDirectory(visit.getPath()) || endsWithChecksum(visit.getPath().getFileName().toString())) {
                        return;
                    }
                    try {
                        result.put(hash(Files.readAllBytes(visit.getPath())), getCoords(visit));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    private static ArtifactCoords getCoords(PathVisit visit) {
        var file = visit.getPath();
        var parent = file.getParent();
        if (parent == null) {
            return null;
        }
        final String version = parent.getFileName().toString();
        parent = parent.getParent();
        if (parent == null || parent.getFileName() == null) {
            return null;
        }
        final String artifactId = parent.getFileName().toString();
        if (parent.getParent() == null) {
            return null;
        }

        var str = file.getFileName().toString();

        String groupId = visit.getRelativePath("/");
        int i = groupId.length() - str.length() - version.length() - artifactId.length() - 3;
        groupId = groupId.substring(0, i).replace("/", ".");

        str = str.substring(artifactId.length() + 1 + version.length());

        final String classifier;
        final String type;
        if (str.charAt(0) == '.') {
            classifier = ArtifactCoords.DEFAULT_CLASSIFIER;
            type = str.substring(1);
        } else if (str.charAt(0) == '-') {
            var dot = str.lastIndexOf('.');
            type = str.substring(dot + 1);
            classifier = str.substring(1, dot);
        } else {
            throw new IllegalArgumentException("Failed to parse artifact coords of " + file.getFileName());
        }
        return ArtifactCoords.of(groupId, artifactId, classifier, type, version);
    }

    private static boolean endsWithChecksum(String name) {
        var dot = name.lastIndexOf('.');
        if (dot < 1) {
            return false;
        }
        return CHECKSUM_SUFFIXES.contains(name.substring(dot + 1));
    }
}
