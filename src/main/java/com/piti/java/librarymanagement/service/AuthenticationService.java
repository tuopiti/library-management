package com.piti.java.librarymanagement.service;

import com.piti.java.librarymanagement.dto.AuthenticationRequest;
import com.piti.java.librarymanagement.dto.AuthenticationResponse;
import com.piti.java.librarymanagement.dto.RegistrationRequest;

import jakarta.mail.MessagingException;

public interface AuthenticationService {
	void register(RegistrationRequest request) throws MessagingException;
	AuthenticationResponse authenticate(AuthenticationRequest request);
	void activateAccount(String token) throws MessagingException;
}
