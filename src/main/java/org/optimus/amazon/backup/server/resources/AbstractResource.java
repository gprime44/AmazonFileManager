package org.optimus.amazon.backup.server.resources;

import org.apache.commons.lang3.StringUtils;
import org.optimus.amazon.backup.server.dto.UserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;

public class AbstractResource {

	@Value("${admin.login}")
	private String ADMIN_LOGIN;

	protected UserDto getUser() throws Exception {
		String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
		if (StringUtils.isEmpty(currentUser)) {
			throw new Exception("User not logged");
		}

		UserDto userDto = new UserDto();
		userDto.setLogin(currentUser);
		userDto.setName(currentUser);
		userDto.setAdmin(StringUtils.equals(currentUser, ADMIN_LOGIN));
		return userDto;
	}

}
