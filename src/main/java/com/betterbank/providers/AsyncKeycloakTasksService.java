package com.betterbank.providers;

import com.betterbank.dto.request.RegisterRequest;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@Service
public class AsyncKeycloakTasksService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncKeycloakTasksService.class);
    private final Keycloak keycloakAdminClient;
    private final String keycloakRealm;

    public AsyncKeycloakTasksService(Keycloak keycloakAdminClient, @Value("${app.config.keycloak.realm}") String keycloakRealm) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.keycloakRealm = keycloakRealm;
    }

    @Async("taskExecutor")
    public void createUserInKeycloak(RegisterRequest registerRequest) {
        LOGGER.info("Starting asynchronous Keycloak User Creation Task for: {}", registerRequest.email());

        String userId = null; // To store the Keycloak user ID

        try {
            UsersResource usersResource = this.keycloakAdminClient.realm(this.keycloakRealm).users();

            // 1. Create UserRepresentation
            UserRepresentation userRepresentation = new UserRepresentation();
            userRepresentation.setEnabled(true);
            userRepresentation.setUsername(registerRequest.email());
            userRepresentation.setEmail(registerRequest.email());
            userRepresentation.setFirstName(registerRequest.firstName());
            userRepresentation.setLastName(registerRequest.lastName());
            userRepresentation.setEmailVerified(false);
            userRepresentation.setRequiredActions(List.of("VERIFY_EMAIL"));

            // 2. Set Credentials
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(registerRequest.password());
            credential.setTemporary(false);

            // 3. set the credentials to the user object
            userRepresentation.setCredentials(Collections.singletonList(credential));

            // 4. Create user in Keycloak
            try (Response response = usersResource.create(userRepresentation)) {
                if (response.getStatus() != HttpStatus.CREATED.value()) {
                    String errorBody = response.readEntity(String.class); // Attempt to read error message from Keycloak
                    LOGGER.error("Failed to create user {} in Keycloak. Status: {}, Error: {}", registerRequest.email(), response.getStatus(), errorBody);
//                    emailService.sendRegistrationCompleteEmail(request.email(), "Registration failed due to a server error. Please try again later.", "Registration Failed");
                    return; // Stop processing on failure
                } else {
                    URI location = response.getLocation();
                    // Extract user ID from the response header (e.g., Location: /auth/admin/realms/{realm}/users/{userId})
                    String path = location.getPath();
                    userId = path.substring(path.lastIndexOf('/') + 1);
                    LOGGER.info("User {} created successfully in Keycloak with ID: {}", registerRequest.email(), userId);

                    LOGGER.info("Sending verification email on the email ID: {}", registerRequest.email());
                    usersResource.get(userId).sendVerifyEmail();
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error during asynchronous Keycloak user creation for {}: {}", registerRequest.email(), e.getMessage(), e);
            // Send failure email if any unexpected error occurs in the async process
//            emailService.sendRegistrationCompleteEmail(request.email(), "An unexpected error occurred during your account registration. Please contact support.", "Registration Error");
        }
    }
}
