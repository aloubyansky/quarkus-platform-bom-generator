package io.quarkus.buildoscope;

import java.util.Objects;

public class MavenMojoExecution implements BuildActor {

    public static final String KIND = "Maven plugin";

    public static class MojoKey {

        public static final MojoKey INSTALL = of("org.apache.maven.plugins", "maven-install-plugin", "install");
        public static final MojoKey COMPILE = of("org.apache.maven.plugins", "maven-compiler-plugin", "compile");

        public static MojoKey of(String pluginGroupId, String pluginArtifactId, String goal) {
            return new MojoKey(pluginGroupId, pluginArtifactId, goal);
        }

        private final String groupId;
        private final String artifactId;
        private final String goal;

        private MojoKey(String groupId, String artifactId, String goal) {
            this.groupId = Objects.requireNonNull(groupId);
            this.artifactId = Objects.requireNonNull(artifactId);
            this.goal = Objects.requireNonNull(goal);
        }

        @Override
        public int hashCode() {
            return Objects.hash(artifactId, goal, groupId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            MojoKey other = (MojoKey) obj;
            return Objects.equals(artifactId, other.artifactId) && Objects.equals(goal, other.goal)
                    && Objects.equals(groupId, other.groupId);
        }

        @Override
        public String toString() {
            return groupId + ":" + artifactId + ":" + goal;
        }
    }

    public static MavenMojoExecution of(String pluginGroupId, String pluginArtifactId, String pluginVersion,
            String goal, String executionId) {
        return new MavenMojoExecution(pluginGroupId, pluginArtifactId, pluginVersion, goal, executionId);
    }

    private final String id;
    private final MojoKey key;

    MavenMojoExecution(String pluginGroupId, String pluginArtifactId, String pluginVersion,
            String goal, String executionId) {
        id = pluginGroupId + ":" + pluginArtifactId + ":" + pluginVersion + ":" + goal + "#" + executionId;
        key = MojoKey.of(pluginGroupId, pluginArtifactId, goal);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getKind() {
        return KIND;
    }

    @Override
    public String toString() {
        return id;
    }

    public MojoKey getKey() {
        return key;
    }

    public boolean matchesKey(MojoKey key) {
        return this.key.equals(key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MavenMojoExecution other = (MavenMojoExecution) obj;
        return Objects.equals(id, other.id);
    }
}
