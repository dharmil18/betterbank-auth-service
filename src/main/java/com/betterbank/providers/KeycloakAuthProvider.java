package com.betterbank.providers;

import com.betterbank.dto.request.LoginRequest;
import com.betterbank.dto.request.RegisterRequest;
import com.betterbank.dto.response.LoginResponse;
import com.betterbank.dto.response.LoginState;
import com.betterbank.dto.response.LoginStatus;
import com.betterbank.dto.response.RegistrationOutcome;
import feign.FeignException;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class KeycloakAuthProvider implements AuthProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakAuthProvider.class);

    private final Keycloak keycloakAdminClient;

    private final AsyncKeycloakTasksService asyncKeycloakTasksService;

    private final KeycloakTokenFeignClient keycloakTokenFeignClient;

    @Value("${app.config.keycloak.url}")
    private String keycloakUrl;

    @Value("${app.config.keycloak.realm}")
    private String keycloakRealm;

    @Value("${app.config.keycloak.client_id}")
    private String keycloakClientId;

    @Value("${app.config.keycloak.client-secret}")
    private String keycloakClientSecret;

    //
//    @Value("${app.config.keycloak.admin.username}")
//    private String keycloakAdminUsername;
//
//    @Value("${app.config.keycloak.admin.password}")
//    private String keycloakAdminPassword;
//
//    @Value("${app.config.keycloak.admin.client-id}")
//    private String keycloakAdminClientId;
//
//    @Value("${app.config.keycloak.admin.client-secret}")
//    private String keycloakAdminClientSecret;

    public KeycloakAuthProvider(Keycloak keycloakAdminClient, AsyncKeycloakTasksService asyncKeycloakTasksService, KeycloakTokenFeignClient keycloakTokenFeignClient) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.asyncKeycloakTasksService = asyncKeycloakTasksService;
        this.keycloakTokenFeignClient = keycloakTokenFeignClient;
    }

    @Override
    public RegistrationOutcome register(RegisterRequest registerRequest) {
        LOGGER.info("Processing registration request for email: {}", registerRequest.email());

        try {
            // Check if the user already exists
            UsersResource usersResource = keycloakAdminClient.realm(keycloakRealm).users();
            LOGGER.debug("Checking is user already exists in Keycloak realm: {}", this.keycloakRealm);
            if (!usersResource.searchByUsername(registerRequest.email(), true).isEmpty() || !usersResource.searchByEmail(registerRequest.email(), true).isEmpty()) {
                LOGGER.warn("Registration failed: User with email {} already exists", registerRequest.email());
                return RegistrationOutcome.USER_EXISTS;
            }

            // call async method here
            LOGGER.info("User does not exist, delegating user creation to AsyncKeycloakTaskService for: {}", registerRequest.email());
            asyncKeycloakTasksService.createUserInKeycloak(registerRequest);

            // return a temp response
            LOGGER.info("Sending a response, further user onboarding will be done on email");
            return RegistrationOutcome.INITIATED_ASYNC_PROCESS;
        } catch (jakarta.ws.rs.ProcessingException e) {
            LOGGER.error("Failed to connect to Keycloak: {}", e.getMessage(), e);
            return RegistrationOutcome.AUTH_PROVIDER_ERROR;
        } catch (jakarta.ws.rs.WebApplicationException e) {
            LOGGER.error("Keycloak returned HTTP error: {} - {}", e.getResponse().getStatus(), e.getMessage(), e);
            return RegistrationOutcome.AUTH_PROVIDER_ERROR;
        } catch (Exception e) {
            LOGGER.error("Unexpected error during registration: {}", e.getMessage(), e);
            return RegistrationOutcome.AUTH_PROVIDER_ERROR;
        }
    }

    @Override
    public LoginStatus login(LoginRequest loginRequest) {
        LOGGER.info("Processing login request for email: {}", loginRequest.email());
        UsersResource usersResource = keycloakAdminClient.realm(keycloakRealm).users();
        // 1. Check if user does not exist
        boolean userExists = !usersResource.searchByUsername(loginRequest.email(), true).isEmpty() || !usersResource.searchByEmail(loginRequest.email(), true).isEmpty();
        if (!userExists) {
            LOGGER.warn("Login failed: User with email {} does not exist", loginRequest.email());
            return new LoginStatus(LoginState.INVALID_CREDENTIALS, Optional.empty(), Optional.empty());
        }
        // 2. Check if user verification is still pending
        var userList = usersResource.searchByEmail(loginRequest.email(), true);
        if (!userList.isEmpty() && userList.get(0).isEnabled() && !userList.get(0).isEmailVerified()) {
            LOGGER.warn("Login failed: User email verification pending for {}", loginRequest.email());
            return new LoginStatus(LoginState.EMAIL_NOT_VERIFIED, Optional.empty(), Optional.empty());
        }

        // 3. If user exists and is verified, get access and refresh token
        MultiValueMap<String, String> formParam = new LinkedMultiValueMap<>();
        formParam.put("grant_type", Collections.singletonList("password"));
        formParam.put("client_id", Collections.singletonList(keycloakClientId));
        formParam.put("client_secret", Collections.singletonList(keycloakClientSecret));
        formParam.put("username", Collections.singletonList(loginRequest.email()));
        formParam.put("password", Collections.singletonList(loginRequest.password()));

        Map<String, Object> tokenResponse = keycloakTokenFeignClient.getToken(keycloakRealm, formParam);
        LOGGER.info("Token response: {}", tokenResponse);
        String accessToken = tokenResponse.get("access_token") != null ? tokenResponse.get("access_token").toString() : null;
        String refreshToken = tokenResponse.get("refresh_token") != null ? tokenResponse.get("refresh_token").toString() : null;

        if (accessToken == null) {
            LOGGER.warn("Login failed: Invalid credentials for {}", loginRequest.email());
            return new LoginStatus(LoginState.INVALID_CREDENTIALS, Optional.empty(), Optional.empty());
        }
        return new LoginStatus(LoginState.LOGGED_IN, Optional.of(accessToken), Optional.ofNullable(refreshToken));
    }
}
