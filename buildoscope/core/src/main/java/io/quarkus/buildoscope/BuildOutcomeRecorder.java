package io.quarkus.buildoscope;

public interface BuildOutcomeRecorder {

    void recordOutcome(BuildActionOutcome outcome);
}
