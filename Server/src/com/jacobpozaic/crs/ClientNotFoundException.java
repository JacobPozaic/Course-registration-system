package com.jacobpozaic.crs;

public class ClientNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ClientNotFoundException(int ID) {
		super("Client could not be found or was not connected: " + ID);
	}
}
