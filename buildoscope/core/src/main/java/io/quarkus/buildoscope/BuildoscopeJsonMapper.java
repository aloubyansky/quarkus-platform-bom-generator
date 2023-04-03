package io.quarkus.buildoscope;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class BuildoscopeJsonMapper {

    private static ObjectMapper mapper;
    private static ObjectMapper yamlMapper;

    public static ObjectMapper mapper() {
        return mapper == null ? mapper = initMapper(new ObjectMapper()) : mapper;
    }

    private static ObjectMapper yamlMapper() {
        return yamlMapper == null ? yamlMapper = initMapper(new ObjectMapper(new YAMLFactory())) : yamlMapper;
    }

    private static ObjectMapper mapperForPath(Path p) {
        return p.getFileName().toString().endsWith("json") ? mapper() : yamlMapper();
    }

    public static ObjectMapper initMapper(ObjectMapper mapper) {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    public static void serialize(Object catalog, Path p) throws IOException {
        serialize(mapperForPath(p), catalog, p);
    }

    public static void serialize(ObjectMapper mapper, Object catalog, Path p) throws IOException {
        final Path parent = p.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(p)) {
            serialize(mapper, catalog, writer);
        }
    }

    public static void serialize(Object catalog, Writer writer) throws IOException {
        serialize(mapper(), catalog, writer);
    }

    public static void serialize(ObjectMapper mapper, Object catalog, Writer writer) throws IOException {
        mapper.writeValue(writer, catalog);
    }

    public static <T> T deserialize(Path p, Class<T> t) throws IOException {
        return deserialize(mapperForPath(p), p, t);
    }

    public static <T> T deserialize(ObjectMapper mapper, Path p, Class<T> t) throws IOException {
        if (!Files.exists(p)) {
            throw new IllegalArgumentException("File " + p + " does not exist");
        }
        try (BufferedReader reader = Files.newBufferedReader(p)) {
            return mapper.readValue(reader, t);
        }
    }

    public static <T> T deserialize(InputStream is, Class<T> t) throws IOException {
        return deserialize(mapper(), is, t);
    }

    public static <T> T deserialize(ObjectMapper mapper, InputStream is, Class<T> t) throws IOException {
        return mapper.readValue(is, t);
    }

    public static <T> T deserialize(Reader reader, Class<T> t) throws IOException {
        return deserialize(mapper(), reader, t);
    }

    public static <T> T deserialize(ObjectMapper mapper, Reader reader, Class<T> t) throws IOException {
        return mapper.readValue(reader, t);
    }
}
