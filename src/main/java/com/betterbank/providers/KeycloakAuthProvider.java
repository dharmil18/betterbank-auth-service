package com.betterbank.providers;

import com.betterbank.dto.request.RegisterRequest;
import com.betterbank.dto.response.RegistrationOutcome;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;


public class KeycloakAuthProvider implements AuthProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakAuthProvider.class);

    private final Keycloak keycloakAdminClient;

    private final AsyncKeycloakTasksService asyncKeycloakTasksService;


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

    public KeycloakAuthProvider(Keycloak keycloakAdminClient, AsyncKeycloakTasksService asyncKeycloakTasksService) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.asyncKeycloakTasksService = asyncKeycloakTasksService;
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

//    @Override
//    public Mono<RegistrationOutcome> register(RegisterRequest registerRequest) {
//        LOG.info("Processing registration request for email: {}", registerRequest.getEmail());
//
//        return Mono.fromCallable(() -> {
//                    UsersResource usersResource = this.keycloakAdminClient.realm(this.keycloakRealm).users();
//
//                    LOG.debug("Checking is user already exists in Keycloak realm: {}", this.keycloakRealm);
//
//                    if (!usersResource.searchByUsername(registerRequest.getEmail(), true).isEmpty() || !usersResource.searchByEmail(registerRequest.getEmail(), true).isEmpty()) {
//                        LOG.warn("Registration failed: User with email {} already exists", registerRequest.getEmail());
//                        return RegistrationOutcome.USER_EXISTS;
//                    }
//
//                    // call async method here
//                    LOG.info("User does not exist, delegating user creation to AsyncKeycloakTaskService for: {}", registerRequest.getEmail());
//                    asyncKeycloakTasksService.createUserInKeycloak(registerRequest);
//
//
//                    // return a temp response
//                    return RegistrationOutcome.INITIATED_ASYNC_PROCESS;
//                }).subscribeOn(Schedulers.boundedElastic())
//                .onErrorResume(error -> {
//                    LOG.error("Registration failed for user {}: {}", registerRequest.getEmail(), error.getMessage());
//                    return Mono.just(RegistrationOutcome.AUTH_PROVIDER_ERROR);
//                });
//    }

//    @Override
//    public Mono<GenericResponse> LOGin(LOGinRequest LOGinRequest) {
//        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakUrl, keycloakRealm);
//        LOG.info("Attempting to LOGin to keycloak at {}", tokenUrl);
//
//        // Prepare form data to send to keycloak
//        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
//        formData.add("grant_type", "password");
//        formData.add("client_id", keycloakClientId);
//        formData.add("client_secret", keycloakClientSecret);
//        formData.add("username", LOGinRequest.getEmail());
//        formData.add("password", LOGinRequest.getPassword());
//
//        return webClient.post().uri(tokenUrl).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(BodyInserters.fromFormData(formData)).retrieve()
//                // FIX: Change method reference to lambda for instance method
//                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> // CORRECTED LINE
//                        clientResponse.bodyToMono(JsonNode.class).<Throwable>flatMap(errorJson -> {
//                            String error = errorJson.has("error") ? errorJson.get("error").asText() : "unknown_error";
//                            String errorDescription = errorJson.has("error_description") ? errorJson.get("error_description").asText() : "No specific error description from Keycloak.";
//                            LOG.warn("Keycloak LOGin client error for {}: Status={}, Error='{}', Description='{}'", LOGinRequest.getEmail(), clientResponse.statusCode(), error, errorDescription);
//
//                            if ("invalid_grant".equals(error) || "invalid_client".equals(error)) {
//                                return Mono.error(new RuntimeException("INVALID_CREDENTIALS"));
//                            } else if ("user_not_verified".equals(error) || errorDescription.contains("Email is not verified")) {
//                                return Mono.error(new RuntimeException("EMAIL_NOT_VERIFIED"));
//                            } else {
//                                return Mono.error(new RuntimeException("KEYCLOAK_AUTH_FAILED: " + errorDescription));
//                            }
//                        }))
//                // FIX: Change method reference to lambda for instance method
//                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> // CORRECTED LINE
//                        clientResponse.bodyToMono(String.class).<Throwable>flatMap(errorBody -> {
//                            LOG.error("Keycloak LOGin server error for {}: Status={}, Body={}", LOGinRequest.getEmail(), clientResponse.statusCode(), errorBody);
//                            return Mono.error(new RuntimeException("KEYCLOAK_SERVER_ERROR"));
//                        })).bodyToMono(JsonNode.class).map(jsonNode -> {
//                    String accessToken = jsonNode.has("access_token") ? jsonNode.get("access_token").asText() : null;
//                    String tokenType = jsonNode.has("token_type") ? jsonNode.get("token_type").asText() : null;
//                    long expiresIn = jsonNode.has("expires_in") ? jsonNode.get("expires_in").asLong() : 0;
//
//                    String message = String.format("LOGin successful. Token type: %s, Expires in: %d seconds. (Access token is %s...)", tokenType, expiresIn, accessToken != null && accessToken.length() > 10 ? accessToken.substring(0, 10) : "N/A");
//                    LOG.info("{}", message);
//                    return new GenericResponse(message);
//                }).onErrorResume(RuntimeException.class, e -> {
//                    String message;
//                    String status = "FAILED";
//
//                    switch (e.getMessage()) {
//                        case "INVALID_CREDENTIALS":
//                            message = "Invalid email or password.";
//                            break;
//                        case "EMAIL_NOT_VERIFIED":
//                            message = "Email not verified. Please check your inbox for a verification link.";
//                            break;
//                        case "KEYCLOAK_SERVER_ERROR":
//                            message = "Keycloak server error during LOGin. Please try again later.";
//                            break;
//                        case "KEYCLOAK_AUTH_FAILED":
//                            message = "Authentication failed due to Keycloak error.";
//                            break;
//                        default:
//                            message = "An unexpected error occurred during LOGin. Please contact support.";
//                            LOG.error("Unhandled error during LOGin: {}", e.getMessage(), e);
//                            break;
//                    }
//                    return Mono.just(new GenericResponse(message));
//                });
//    }
}
