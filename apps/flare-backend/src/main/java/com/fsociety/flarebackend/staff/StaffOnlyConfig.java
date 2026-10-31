package com.fsociety.flarebackend.staff;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Checks the staff_member row before every /api/staff/** handler, so no staff endpoint can forget it.
 * Spring Security has already verified the token; this adds "is an active staff member".
 */
@Configuration
@RequiredArgsConstructor
public class StaffOnlyConfig implements WebMvcConfigurer {

	private final CurrentStaff currentStaff;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new HandlerInterceptor() {
			@Override
			public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
				currentStaff.require();
				return true;
			}
		}).addPathPatterns("/api/staff/**");
	}
}
