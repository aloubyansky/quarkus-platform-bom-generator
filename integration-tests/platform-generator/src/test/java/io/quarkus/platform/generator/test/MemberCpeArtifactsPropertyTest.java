package io.quarkus.platform.generator.test;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.bom.decomposer.maven.platformgen.CpeArtifactsEncoder;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.platform.generator.PlatformTestProjectGenerator;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MemberCpeArtifactsPropertyTest {

    private static final String CAMEL_CPE = "cpe:2.3:a:acme:camel_quarkus:1.0:*:*:*:*:*:*:*";

    @TempDir
    Path workingDir;

    @Test
    void memberCpeArtifactsPropertyIsGeneratedWhenOfferingIsSet() throws Exception {
        var platformGenerator = PlatformTestProjectGenerator.newInstance();

        var camelProject = platformGenerator.configureProject("org.camel", "camel-parent", "1.0")
                .setScm("https://camel.org", "1.0");
        var camelAtom = camelProject.addQuarkusExtensionRuntimeModule("camel-atom");
        var camelAtomDeployment = camelProject.addModule("camel-atom-deployment")
                .addDependency(camelAtom);
        var camelBom = camelProject.addPomModule("camel-bom")
                .addVersionConstraint(camelAtom)
                .addVersionConstraint(camelAtomDeployment);

        platformGenerator.configureMember(camelBom)
                .setName("Camel")
                .setCpe(CAMEL_CPE)
                .setOffering("camel")
                .addExtensionMetadata(
                        ArtifactCoords.jar(camelAtom.getGroupId(), camelAtom.getArtifactId(), camelAtom.getVersion()),
                        "camel-support", true);

        var platform = platformGenerator
                .setProjectDir(workingDir)
                .generateProject()
                .build();

        var camel = platform.getMember("Camel");
        var cpeArtifactsKey = "platform." + PlatformTestProjectGenerator.DEFAULT_GROUP_ID
                + ".quarkus-camel-bom.cpe-artifacts";

        assertThat(camel.getProperties()).containsKey(cpeArtifactsKey);

        String encoded = camel.getProperties().getProperty(cpeArtifactsKey);
        Map<ArtifactCoords, List<ArtifactCoords>> decoded = CpeArtifactsEncoder.decode(encoded);
        assertThat(decoded).isNotEmpty();

        var keyPrefix = "platform." + PlatformTestProjectGenerator.DEFAULT_GROUP_ID + ".quarkus-camel-bom.";
        assertThat(camel.getProperties().getProperty(keyPrefix + "product-name")).isEqualTo("Camel");

        // the map is keyed by the runtime extension artifact
        var runtimeKey = decoded.keySet().stream()
                .filter(coords -> coords.getArtifactId().equals("camel-atom"))
                .findFirst()
                .orElse(null);
        assertThat(runtimeKey)
                .as("Decoded map should be keyed by the camel-atom runtime artifact")
                .isNotNull();

        // the value should contain the corresponding deployment artifact
        assertThat(decoded.get(runtimeKey).stream()
                .anyMatch(coords -> coords.getArtifactId().equals("camel-atom-deployment")))
                .as("The runtime artifact should map to the camel-atom-deployment artifact")
                .isTrue();
    }

    @Test
    void memberWithoutOfferingDoesNotGenerateCpeArtifacts() throws Exception {
        var platformGenerator = PlatformTestProjectGenerator.newInstance();

        var camelProject = platformGenerator.configureProject("org.camel", "camel-parent", "1.0")
                .setScm("https://camel.org", "1.0");
        var camelAtom = camelProject.addQuarkusExtensionRuntimeModule("camel-atom");
        var camelBom = camelProject.addPomModule("camel-bom")
                .addVersionConstraint(camelAtom);

        platformGenerator.configureMember(camelBom)
                .setName("Camel")
                .setCpe(CAMEL_CPE);

        var platform = platformGenerator
                .setProjectDir(workingDir)
                .generateProject()
                .build();

        var camel = platform.getMember("Camel");
        assertThat(camel.getProperties().stringPropertyNames())
                .noneMatch(name -> name.endsWith(".cpe-artifacts"));
    }

    @Test
    void conditionalDependencyInSameMemberWithoutOfferingIsRecordedAsTopLevelDependency() throws Exception {
        var platformGenerator = PlatformTestProjectGenerator.newInstance();

        var camelProject = platformGenerator.configureProject("org.camel", "camel-parent", "1.0")
                .setScm("https://camel.org", "1.0");
        var camelKafka = camelProject.addQuarkusExtensionRuntimeModule("camel-kafka");
        var camelKafkaDeployment = camelProject.addModule("camel-kafka-deployment")
                .addDependency(camelKafka);
        var camelAtom = camelProject.addQuarkusExtensionRuntimeModule("camel-atom")
                .setConditionalDependencies(
                        camelKafka.getGroupId() + ":" + camelKafka.getArtifactId() + ":" + camelKafka.getVersion());
        var camelAtomDeployment = camelProject.addModule("camel-atom-deployment")
                .addDependency(camelAtom);
        var camelBom = camelProject.addPomModule("camel-bom")
                .addVersionConstraint(camelAtom)
                .addVersionConstraint(camelAtomDeployment)
                .addVersionConstraint(camelKafka)
                .addVersionConstraint(camelKafkaDeployment);

        // camel-atom is in offering "camel", camel-kafka is in the catalog but has no offering configured
        platformGenerator.configureMember(camelBom)
                .setName("Camel")
                .setCpe(CAMEL_CPE)
                .setOffering("camel")
                .addExtensionMetadata(
                        ArtifactCoords.jar(camelAtom.getGroupId(), camelAtom.getArtifactId(), camelAtom.getVersion()),
                        "camel-support", true)
                .addExtensionMetadata(
                        ArtifactCoords.jar(camelKafka.getGroupId(), camelKafka.getArtifactId(), camelKafka.getVersion()),
                        "catalog-entry", true);

        var platform = platformGenerator
                .setProjectDir(workingDir)
                .generateProject()
                .build();

        var camel = platform.getMember("Camel");
        var cpeArtifactsKey = "platform." + PlatformTestProjectGenerator.DEFAULT_GROUP_ID
                + ".quarkus-camel-bom.cpe-artifacts";

        assertThat(camel.getProperties()).containsKey(cpeArtifactsKey);
        String encoded = camel.getProperties().getProperty(cpeArtifactsKey);
        Map<ArtifactCoords, List<ArtifactCoords>> decoded = CpeArtifactsEncoder.decode(encoded);

        // Both camel-atom and camel-kafka should be recorded as top-level keys
        var atomKey = decoded.keySet().stream()
                .filter(coords -> coords.getArtifactId().equals("camel-atom"))
                .findFirst()
                .orElse(null);
        assertThat(atomKey).as("camel-atom should be recorded as a top-level key").isNotNull();

        var kafkaKey = decoded.keySet().stream()
                .filter(coords -> coords.getArtifactId().equals("camel-kafka"))
                .findFirst()
                .orElse(null);
        assertThat(kafkaKey).as("camel-kafka conditional dependency should be recorded as a top-level key").isNotNull();

        // camel-atom closure contains camel-atom-deployment, but NOT camel-kafka-deployment
        assertThat(decoded.get(atomKey).stream()
                .anyMatch(coords -> coords.getArtifactId().equals("camel-atom-deployment")))
                .isTrue();
        assertThat(decoded.get(atomKey).stream()
                .noneMatch(coords -> coords.getArtifactId().equals("camel-kafka-deployment")))
                .as("camel-atom dependency list should not contain camel-kafka-deployment")
                .isTrue();

        // camel-kafka closure contains camel-kafka-deployment
        assertThat(decoded.get(kafkaKey).stream()
                .anyMatch(coords -> coords.getArtifactId().equals("camel-kafka-deployment")))
                .as("camel-kafka dependency list should contain camel-kafka-deployment")
                .isTrue();
    }

    @Test
    void crossMemberConditionalDependencyWithoutCpeIsRecordedUnderImportingMember() throws Exception {
        var platformGenerator = PlatformTestProjectGenerator.newInstance();

        var amqProject = platformGenerator.configureProject("org.amq", "amq-parent", "1.0")
                .setScm("https://amq.org", "1.0");
        var amqJms = amqProject.addQuarkusExtensionRuntimeModule("amq-jms");
        var amqJmsDeployment = amqProject.addModule("amq-jms-deployment")
                .addDependency(amqJms);
        var amqBom = amqProject.addPomModule("amq-bom")
                .addVersionConstraint(amqJms)
                .addVersionConstraint(amqJmsDeployment);

        var camelProject = platformGenerator.configureProject("org.camel", "camel-parent", "1.0")
                .setScm("https://camel.org", "1.0");
        var camelAtom = camelProject.addQuarkusExtensionRuntimeModule("camel-atom")
                .setConditionalDependencies(amqJms.getGroupId() + ":" + amqJms.getArtifactId() + ":" + amqJms.getVersion());
        var camelAtomDeployment = camelProject.addModule("camel-atom-deployment")
                .addDependency(camelAtom);
        var camelBom = camelProject.addPomModule("camel-bom")
                .addVersionConstraint(camelAtom)
                .addVersionConstraint(camelAtomDeployment);

        // Camel has CPE and offering, AMQ does not have CPE
        platformGenerator.configureMember(camelBom)
                .setName("Camel")
                .setCpe(CAMEL_CPE)
                .setOffering("camel")
                .addExtensionMetadata(
                        ArtifactCoords.jar(camelAtom.getGroupId(), camelAtom.getArtifactId(), camelAtom.getVersion()),
                        "camel-support", true);
        platformGenerator.configureMember(amqBom)
                .setName("AMQ")
                .addExtensionMetadata(
                        ArtifactCoords.jar(amqJms.getGroupId(), amqJms.getArtifactId(), amqJms.getVersion()),
                        "catalog-entry", true);

        var platform = platformGenerator
                .setProjectDir(workingDir)
                .generateProject()
                .build();

        var camel = platform.getMember("Camel");
        var cpeArtifactsKey = "platform." + PlatformTestProjectGenerator.DEFAULT_GROUP_ID
                + ".quarkus-camel-bom.cpe-artifacts";

        assertThat(camel.getProperties()).containsKey(cpeArtifactsKey);
        String encoded = camel.getProperties().getProperty(cpeArtifactsKey);
        Map<ArtifactCoords, List<ArtifactCoords>> decoded = CpeArtifactsEncoder.decode(encoded);

        // amq-jms is recorded under Camel since AMQ has no CPE
        var amqKey = decoded.keySet().stream()
                .filter(coords -> coords.getArtifactId().equals("amq-jms"))
                .findFirst()
                .orElse(null);
        assertThat(amqKey).as("amq-jms should be recorded under Camel's cpe-artifacts").isNotNull();

        assertThat(decoded.get(amqKey).stream()
                .anyMatch(coords -> coords.getArtifactId().equals("amq-jms-deployment")))
                .as("amq-jms deployment artifact should be present in its deployment closure")
                .isTrue();
    }

    @Test
    void crossMemberConditionalDependencyWithCpeIsSkippedForImportingMember() throws Exception {
        var platformGenerator = PlatformTestProjectGenerator.newInstance();

        var amqProject = platformGenerator.configureProject("org.amq", "amq-parent", "1.0")
                .setScm("https://amq.org", "1.0");
        var amqJms = amqProject.addQuarkusExtensionRuntimeModule("amq-jms");
        var amqJmsDeployment = amqProject.addModule("amq-jms-deployment")
                .addDependency(amqJms);
        var amqBom = amqProject.addPomModule("amq-bom")
                .addVersionConstraint(amqJms)
                .addVersionConstraint(amqJmsDeployment);

        var camelProject = platformGenerator.configureProject("org.camel", "camel-parent", "1.0")
                .setScm("https://camel.org", "1.0");
        var camelAtom = camelProject.addQuarkusExtensionRuntimeModule("camel-atom")
                .setConditionalDependencies(amqJms.getGroupId() + ":" + amqJms.getArtifactId() + ":" + amqJms.getVersion());
        var camelAtomDeployment = camelProject.addModule("camel-atom-deployment")
                .addDependency(camelAtom);
        var camelBom = camelProject.addPomModule("camel-bom")
                .addVersionConstraint(camelAtom)
                .addVersionConstraint(camelAtomDeployment);

        // Both Camel and AMQ declare CPE and offerings
        String amqCpe = "cpe:2.3:a:acme:amq:1.0:*:*:*:*:*:*:*";
        platformGenerator.configureMember(camelBom)
                .setName("Camel")
                .setCpe(CAMEL_CPE)
                .setOffering("camel")
                .addExtensionMetadata(
                        ArtifactCoords.jar(camelAtom.getGroupId(), camelAtom.getArtifactId(), camelAtom.getVersion()),
                        "camel-support", true);
        platformGenerator.configureMember(amqBom)
                .setName("AMQ")
                .setCpe(amqCpe)
                .setOffering("amq")
                .addExtensionMetadata(
                        ArtifactCoords.jar(amqJms.getGroupId(), amqJms.getArtifactId(), amqJms.getVersion()),
                        "amq-support", true);

        var platform = platformGenerator
                .setProjectDir(workingDir)
                .generateProject()
                .build();

        var camel = platform.getMember("Camel");
        var camelCpeArtifactsKey = "platform." + PlatformTestProjectGenerator.DEFAULT_GROUP_ID
                + ".quarkus-camel-bom.cpe-artifacts";
        String camelEncoded = camel.getProperties().getProperty(camelCpeArtifactsKey);
        Map<ArtifactCoords, List<ArtifactCoords>> camelDecoded = CpeArtifactsEncoder.decode(camelEncoded);

        // amq-jms is skipped for Camel because AMQ has its own offering with CPE
        assertThat(camelDecoded.keySet().stream()
                .noneMatch(coords -> coords.getArtifactId().equals("amq-jms")))
                .as("amq-jms should be skipped for Camel")
                .isTrue();

        // but amq-jms is recorded under AMQ
        var amq = platform.getMember("AMQ");
        var amqCpeArtifactsKey = "platform." + PlatformTestProjectGenerator.DEFAULT_GROUP_ID
                + ".quarkus-amq-bom.cpe-artifacts";
        String amqEncoded = amq.getProperties().getProperty(amqCpeArtifactsKey);
        Map<ArtifactCoords, List<ArtifactCoords>> amqDecoded = CpeArtifactsEncoder.decode(amqEncoded);
        assertThat(amqDecoded.keySet().stream()
                .anyMatch(coords -> coords.getArtifactId().equals("amq-jms")))
                .as("amq-jms should be recorded under AMQ's cpe-artifacts")
                .isTrue();
    }

    @Test
    void conditionalDependencyNotInPlatformCatalogIsSkipped() throws Exception {
        var platformGenerator = PlatformTestProjectGenerator.newInstance();

        var camelProject = platformGenerator.configureProject("org.camel", "camel-parent", "1.0")
                .setScm("https://camel.org", "1.0");
        var camelAtom = camelProject.addQuarkusExtensionRuntimeModule("camel-atom")
                .setConditionalDependencies("org.external:external-ext:1.0");
        var camelAtomDeployment = camelProject.addModule("camel-atom-deployment")
                .addDependency(camelAtom);
        var camelBom = camelProject.addPomModule("camel-bom")
                .addVersionConstraint(camelAtom)
                .addVersionConstraint(camelAtomDeployment);

        platformGenerator.configureMember(camelBom)
                .setName("Camel")
                .setCpe(CAMEL_CPE)
                .setOffering("camel")
                .addExtensionMetadata(
                        ArtifactCoords.jar(camelAtom.getGroupId(), camelAtom.getArtifactId(), camelAtom.getVersion()),
                        "camel-support", true);

        var platform = platformGenerator
                .setProjectDir(workingDir)
                .generateProject()
                .build();

        var camel = platform.getMember("Camel");
        var cpeArtifactsKey = "platform." + PlatformTestProjectGenerator.DEFAULT_GROUP_ID
                + ".quarkus-camel-bom.cpe-artifacts";
        String encoded = camel.getProperties().getProperty(cpeArtifactsKey);
        Map<ArtifactCoords, List<ArtifactCoords>> decoded = CpeArtifactsEncoder.decode(encoded);

        // external-ext is not in any platform catalog, so it should be skipped
        assertThat(decoded.keySet().stream()
                .noneMatch(coords -> coords.getArtifactId().equals("external-ext")))
                .as("external-ext should be skipped because it is not in the platform catalog")
                .isTrue();
    }
}
