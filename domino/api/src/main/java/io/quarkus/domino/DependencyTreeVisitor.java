package io.quarkus.domino;

import io.quarkus.bom.decomposer.ScmRevisionResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.domino.scm.ScmRevision;
import io.quarkus.maven.dependency.ArtifactCoords;
import java.util.List;
import org.eclipse.aether.repository.RemoteRepository;

public interface DependencyTreeVisitor {

    interface DependencyVisit {

        ScmRevision getRevision();

        ArtifactCoords getCoords();

        List<RemoteRepository> getRepositories();

        boolean isManaged();
    }

    interface VisitorInitializationContext {

        ProjectDependencyConfig getConfig();

        MavenArtifactResolver getArtifactResolver();

        ScmRevisionResolver getScmRevisionResolver();
    }

    void beforeAllRoots(VisitorInitializationContext initCtx);

    void afterAllRoots();

    void enterRootArtifact(DependencyVisit visit);

    void leaveRootArtifact(DependencyVisit visit);

    void enterDependency(DependencyVisit visit);

    void leaveDependency(DependencyVisit visit);

    void enterParentPom(DependencyVisit visit);

    void leaveParentPom(DependencyVisit visit);

    void enterBomImport(DependencyVisit visit);

    void leaveBomImport(DependencyVisit visit);
}
