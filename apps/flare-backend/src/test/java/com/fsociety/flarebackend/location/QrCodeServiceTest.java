package com.fsociety.flarebackend.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Resolving the scanned QR code ID (pipeline.md step 3). Anything odd is UNKNOWN, never an error. */
class QrCodeServiceTest {

	private final QrCodeRepository repository = mock(QrCodeRepository.class);
	private final QrCodeService service = new QrCodeService(repository);

	@Test
	void anActiveQrCodeGivesItsLocation() {
		Location location = new Location();
		when(repository.findById("skp4fq3r5e")).thenReturn(Optional.of(qrCode(QrCodeStatus.ACTIVE, location)));

		QrCodeService.Resolution resolution = service.resolve("skp4fq3r5e");

		assertThat(resolution.getStatus()).isEqualTo(QrCodeService.Status.ACTIVE);
		assertThat(resolution.getLocation()).contains(location);
	}

	@Test
	void aMutedQrCodeIsMutedButKeepsItsLocation() {
		Location location = new Location();
		when(repository.findById("9wa6zzsz5d")).thenReturn(Optional.of(qrCode(QrCodeStatus.MUTED, location)));

		QrCodeService.Resolution resolution = service.resolve("9wa6zzsz5d");

		assertThat(resolution.getStatus()).isEqualTo(QrCodeService.Status.MUTED);
		assertThat(resolution.getLocation()).contains(location);
	}

	@Test
	void retiredAndMissingQrCodesAreUnknown() {
		when(repository.findById("retired01")).thenReturn(Optional.of(qrCode(QrCodeStatus.RETIRED, new Location())));
		when(repository.findById("nosuchcode")).thenReturn(Optional.empty());

		assertThat(service.resolve("retired01").getStatus()).isEqualTo(QrCodeService.Status.UNKNOWN);
		assertThat(service.resolve("retired01").getLocation()).isEmpty();
		assertThat(service.resolve("nosuchcode").getStatus()).isEqualTo(QrCodeService.Status.UNKNOWN);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "SKP4FQ3R5E", "abc-123", "abc 123", "<script>", "a234567890123456789012345678901234" })
	void badlyFormedIdsAreUnknownWithoutADatabaseLookup(String id) {
		assertThat(service.resolve(id).getStatus()).isEqualTo(QrCodeService.Status.UNKNOWN);
		verify(repository, never()).findById(anyString());
	}

	private static QrCode qrCode(QrCodeStatus status, Location location) {
		QrCode qrCode = new QrCode();
		qrCode.setStatus(status);
		qrCode.setLocation(location);
		return qrCode;
	}
}
