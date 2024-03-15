package io.quarkus.domino.cli;

import io.quarkus.domino.ArtifactSet;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(name = "trace", header = "Trace dependencies matching a pattern to specified root artifacts", description = "%n"
        + "This command will search for artifact matching a pattern among dependencies of specified root artifacts.")
public class Trace extends BaseDependencyCommand {

    @CommandLine.Option(names = {
            "--dependency" }, description = "Trace artifacts matching specified glob patterns as dependencies", split = ",", required = true)
    protected List<String> patterns = List.of();

    @Override
    public Integer call() throws Exception {

        final ArtifactSet tracePattern = compilePatterns();

        var resolver = getResolver();

        var roots = getRoots();
        for (var root : roots) {
            log().info(root.getArtifact().toString());
        }
        return 0;
    }

    private ArtifactSet compilePatterns() {
        var builder = ArtifactSet.builder();
        for (var exp : patterns) {
            builder.include(exp);
        }
        return builder.build();
    }
}
