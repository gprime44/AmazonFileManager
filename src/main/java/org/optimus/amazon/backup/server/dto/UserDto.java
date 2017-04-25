package org.optimus.amazon.backup.server.dto;

public class UserDto {

	private String login;

	private String name;

	private boolean admin;

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isAdmin() {
		return admin;
	}

	public void setAdmin(boolean admin) {
		this.admin = admin;
	}

	@Override
	public String toString() {
		return "UserDto [login=" + login + ", name=" + name + ", admin=" + admin + "]";
	}
}
