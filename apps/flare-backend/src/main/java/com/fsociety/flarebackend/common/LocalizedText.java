package com.fsociety.flarebackend.common;

import lombok.Value;

/** Text in both UI languages (contract schema {@code LocalizedText}). */
@Value
public class LocalizedText {
	String ro;
	String en;
}
