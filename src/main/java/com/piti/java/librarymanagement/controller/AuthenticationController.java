package com.piti.java.librarymanagement.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.piti.java.librarymanagement.dto.AuthenticationRequest;
import com.piti.java.librarymanagement.dto.AuthenticationResponse;
import com.piti.java.librarymanagement.dto.RegistrationRequest;
import com.piti.java.librarymanagement.service.AuthenticationService;

import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
//@Tag(name = "Authentication")
public class AuthenticationController {
	private final AuthenticationService authenticationService;
	
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<?> register(@RequestBody @Valid RegistrationRequest request) throws MessagingException {
    	authenticationService.register(request);
        return ResponseEntity.accepted().build();
    }
    
    
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }
    
    @GetMapping("/activate-account")
    public void confirm(@RequestParam String token) throws MessagingException {
    	authenticationService.activateAccount(token);
    }
}
