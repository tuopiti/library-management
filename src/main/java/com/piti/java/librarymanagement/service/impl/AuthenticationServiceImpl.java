package com.piti.java.librarymanagement.service.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.piti.java.librarymanagement.config.security.jwt.JwtService;
import com.piti.java.librarymanagement.dto.AuthenticationRequest;
import com.piti.java.librarymanagement.dto.AuthenticationResponse;
import com.piti.java.librarymanagement.dto.RegistrationRequest;
import com.piti.java.librarymanagement.email.EmailService;
import com.piti.java.librarymanagement.email.EmailTemplateName;
import com.piti.java.librarymanagement.model.Token;
import com.piti.java.librarymanagement.model.User;
import com.piti.java.librarymanagement.repository.RoleRepository;
import com.piti.java.librarymanagement.repository.TokenRepository;
import com.piti.java.librarymanagement.repository.UserRepository;
import com.piti.java.librarymanagement.service.AuthenticationService;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService{
	private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    
    @Value("${application.mailing.frontend.activation-url}")
    private String activationUrl;
    

	@Override
	public void register(RegistrationRequest request) throws MessagingException {
		var userRole = roleRepository.findByName("USER")
		// todo - better exception handling
		.orElseThrow(() -> new IllegalStateException("ROLE USER was not initiated"));
		var user = User.builder()
		        .firstname(request.getFirstname())
		        .lastname(request.getLastname())
		        .email(request.getEmail())
		        .password(passwordEncoder.encode(request.getPassword()))
		        .accountLocked(false)
		        .enabled(false)
		        .roles(List.of(userRole))
		        .build();
		userRepository.save(user);
		sendValidationEmail(user);
	}
	
	
	@Override
	public AuthenticationResponse authenticate(AuthenticationRequest request) {
		   var auth = authenticationManager.authenticate(
	                new UsernamePasswordAuthenticationToken(
	                        request.getEmail(),
	                        request.getPassword()
	                )
	        );

	        var claims = new HashMap<String, Object>();
	        var user = ((User) auth.getPrincipal());
	        claims.put("fullName", user.getFullName());

	        var jwtToken = jwtService.generateToken(claims, (User) auth.getPrincipal());
	        return AuthenticationResponse.builder()
	                .token(jwtToken)
	                .build();
	}
	
	
	//@Transactional
	@Override
	public void activateAccount(String token) throws MessagingException {
		Token savedToken = tokenRepository.findByToken(token)
                // todo exception has to be defined
                .orElseThrow(() -> new RuntimeException("Invalid token"));
        if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())) {
            sendValidationEmail(savedToken.getUser());
            throw new RuntimeException("Activation token has expired. A new token has been send to the same email address");
        }

        var user = userRepository.findById(savedToken.getUser().getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);

        savedToken.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(savedToken);
		
	}

	

    private void sendValidationEmail(User user) throws MessagingException {
        var newToken = generateAndSaveActivationToken(user);

        emailService.sendEmail(
                user.getEmail(),
                user.getFullName(),
                EmailTemplateName.ACTIVATE_ACCOUNT,
                activationUrl,
                newToken,
                "Account activation"
                );
    }
    
	private String generateAndSaveActivationToken(User user) {
        // Generate a token
        String generatedToken = generateActivationCode(6);
        var token = Token.builder()
                .token(generatedToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build();
        tokenRepository.save(token);

        return generatedToken;
    }

    private String generateActivationCode(int length) {
        String characters = "0123456789";
        StringBuilder codeBuilder = new StringBuilder();

        SecureRandom secureRandom = new SecureRandom();

        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }

        return codeBuilder.toString();
    }


}
