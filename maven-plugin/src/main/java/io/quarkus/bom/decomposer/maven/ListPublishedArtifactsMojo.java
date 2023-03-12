package io.quarkus.bom.decomposer.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "listPublishedArtifacts")
public class ListPublishedArtifactsMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        project.getAttachedArtifacts().forEach(a -> {
            getLog().info("Published: " + a);
        });
    }
}
