package com.betterbank.providers;

import com.betterbank.dto.request.RegisterRequest;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class AsyncKeycloakTasksService {
    private final Keycloak keycloakAdminClient;
    private final String keycloakRealm;

    public AsyncKeycloakTasksService(Keycloak keycloakAdminClient, @Value("${app.config.keycloak.realm}") String keycloakRealm) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.keycloakRealm = keycloakRealm;
    }

    @Async("taskExecutor")
    public void createUserInKeycloak(RegisterRequest registerRequest) {
        log.info("Starting asynchronous Keycloak User Creation Task for: {}", registerRequest.getEmail());

        String userId = null; // To store the Keycloak user ID

        try {
            UsersResource usersResource = this.keycloakAdminClient.realm(this.keycloakRealm).users();

//            // 1. Idempotency Check (Defensive: prevents issues if async task retries or race conditions)
//            List<UserRepresentation> existingUsers = usersResource.searchByEmail(registerRequest.getEmail(), true);
//            if (!existingUsers.isEmpty()) {
//                log.warn("Asynchronous creation skipped: User with email {} already exists in Keycloak (ID: {}).", registerRequest.getEmail(), existingUsers.get(0).getId());
////                emailService.sendRegistrationCompleteEmail(request.getEmail(), "Your account was already active.", "Registration Already Active");
//                return;
//            }

            // 2. Create UserRepresentation
            UserRepresentation userRepresentation = new UserRepresentation();
            userRepresentation.setEnabled(true);
            userRepresentation.setUsername(registerRequest.getEmail());
            userRepresentation.setEmail(registerRequest.getEmail());
            userRepresentation.setFirstName(registerRequest.getFirstName());
            userRepresentation.setLastName(registerRequest.getLastName());
            userRepresentation.setEmailVerified(false);
            userRepresentation.setRequiredActions(List.of("VERIFY_EMAIL"));

            // 3. Set Credentials
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(registerRequest.getPassword());
            credential.setTemporary(false);

            // set the credentials to the user object
            userRepresentation.setCredentials(Collections.singletonList(credential));

            // 4. Create user in Keycloak
            try (Response response = usersResource.create(userRepresentation)) {
                if (response.getStatus() != HttpStatus.CREATED.value()) {
                    String errorBody = response.readEntity(String.class); // Attempt to read error message from Keycloak
                    log.error("Failed to create user {} in Keycloak. Status: {}, Error: {}", registerRequest.getEmail(), response.getStatus(), errorBody);
//                    emailService.sendRegistrationCompleteEmail(request.getEmail(), "Registration failed due to a server error. Please try again later.", "Registration Failed");
                    return; // Stop processing on failure
                } else {
                    URI location = response.getLocation();
                    // Extract user ID from the response header (e.g., Location: /auth/admin/realms/{realm}/users/{userId})
                    String path = location.getPath();
                    userId = path.substring(path.lastIndexOf('/') + 1);
                    log.info("User {} created successfully in Keycloak with ID: {}", registerRequest.getEmail(), userId);

                    log.info("Sending verification email on the email ID: {}", registerRequest.getEmail());
                    usersResource.get(userId).sendVerifyEmail();
                }
            }

        } catch (Exception e) {
            log.error("Error during asynchronous Keycloak user creation for {}: {}", registerRequest.getEmail(), e.getMessage(), e);
            // Send failure email if any unexpected error occurs in the async process
//            emailService.sendRegistrationCompleteEmail(request.getEmail(), "An unexpected error occurred during your account registration. Please contact support.", "Registration Error");
        }
    }
}
