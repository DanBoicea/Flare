package com.fsociety.flarebackend.staff;

import java.util.UUID;

import com.fsociety.flarebackend.common.ErrorCode;
import com.fsociety.flarebackend.common.FlareException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * The staff member making the current request: the verified JWT's "sub" looked up in staff_member.
 * Roles and Teams come from our tables, never from JWT claims. Loaded once per request.
 */
@Component
@RequiredArgsConstructor
public class CurrentStaff {

	private static final String REQUEST_ATTRIBUTE = CurrentStaff.class.getName();

	private final StaffMemberRepository staffMembers;

	/** The active staff member, or 403 NOT_A_STAFF_MEMBER for a valid token with no active staff_member row. */
	public StaffMember require() {
		RequestAttributes request = RequestContextHolder.currentRequestAttributes();
		if (request.getAttribute(REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST) instanceof StaffMember cached) {
			return cached;
		}
		StaffMember member = staffMembers.findById(subject())
				.filter(StaffMember::isActive)
				.orElseThrow(() -> new FlareException(ErrorCode.NOT_A_STAFF_MEMBER));
		request.setAttribute(REQUEST_ATTRIBUTE, member, RequestAttributes.SCOPE_REQUEST);
		return member;
	}

	private static UUID subject() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication instanceof JwtAuthenticationToken token)) {
			throw new FlareException(ErrorCode.UNAUTHORIZED);
		}
		try {
			return UUID.fromString(token.getToken().getSubject());
		}
		catch (IllegalArgumentException | NullPointerException e) {
			// Supabase user IDs are UUIDs; anything else can't be a staff member.
			throw new FlareException(ErrorCode.NOT_A_STAFF_MEMBER);
		}
	}
}
