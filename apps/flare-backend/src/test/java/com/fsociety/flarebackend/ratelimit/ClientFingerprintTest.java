package com.fsociety.flarebackend.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Device and network identifiers are stored only as keyed hashes (ADR-0018). */
class ClientFingerprintTest {

	@Test
	void isAStableHexHmac() {
		ClientFingerprint fingerprint = new ClientFingerprint("secret-one");

		String hash = fingerprint.hash("3f0e8f8e-2c4b-4d6c-9a51-6f1f3f0c2b7a");

		assertThat(hash).matches("[0-9a-f]{64}").isEqualTo(fingerprint.hash("3f0e8f8e-2c4b-4d6c-9a51-6f1f3f0c2b7a"));
	}

	@Test
	void neverContainsTheOriginalValue() {
		assertThat(new ClientFingerprint("secret-one").hash("192.168.1.10")).doesNotContain("192.168");
	}

	@Test
	void differentSecretsGiveDifferentHashes() {
		assertThat(new ClientFingerprint("secret-one").hash("192.168.1.10"))
				.isNotEqualTo(new ClientFingerprint("secret-two").hash("192.168.1.10"));
	}

	@Test
	void knownValue() {
		// HMAC-SHA256(key = "key", message = "The quick brown fox jumps over the lazy dog"), a published test vector.
		assertThat(new ClientFingerprint("key").hash("The quick brown fox jumps over the lazy dog"))
				.isEqualTo("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8");
	}
}
