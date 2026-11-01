package com.fsociety.flarebackend.ratelimit;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Keyed hash (HMAC-SHA256) of a device ID or network address (ADR-0018). Only the hash is stored, so the
 * database never holds raw device IDs or IP addresses, and without the secret they can't be guessed back.
 */
public class ClientFingerprint {

	private static final String ALGORITHM = "HmacSHA256";

	private final SecretKeySpec key;

	public ClientFingerprint(String secret) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalArgumentException("flare.security.hash-secret is required");
		}
		this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
	}

	public String hash(String value) {
		try {
			// Mac isn't thread-safe; a new one per call is cheap at demo scale.
			Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(key);
			return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (GeneralSecurityException e) {
			throw new IllegalStateException(ALGORITHM + " is not available", e);
		}
	}
}
