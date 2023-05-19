package io.quarkus.buildoscope;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileDigestHasher implements FileHasher {

    public static FileDigestHasher sha256() {
        return of("SHA-256");
    }

    public static FileDigestHasher of(String alg) {
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance(alg);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Failed to locate message digest implementation for algorithm " + alg);
        }
        return new FileDigestHasher(md);
    }

    private final MessageDigest digest;

    protected FileDigestHasher(MessageDigest digest) {
        this.digest = digest;
    }

    @Override
    public String hash(Path p) {
        final byte[] bytes;
        try {
            bytes = Files.readAllBytes(p);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + p, e);
        }
        return toHashString(digest.digest(bytes));
    }

    private static String toHashString(byte[] digest) {
        var sb = new StringBuilder(40);
        for (byte b : digest) {
            sb.append(Integer.toHexString((b & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }
}
