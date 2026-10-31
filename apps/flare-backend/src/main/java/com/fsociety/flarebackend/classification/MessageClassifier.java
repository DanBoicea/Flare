package com.fsociety.flarebackend.classification;

/**
 * Decides what a Passenger message is (backend.md, "Seams between Track A and Track C"). Track C adds the
 * LLM implementation (ADR-0022); until then {@link FallbackClassifier} is the only one. Callers catch every
 * exception and use the fallback, so an implementation may simply throw on failure.
 */
public interface MessageClassifier {

	Classification classify(ClassificationInput input);
}
