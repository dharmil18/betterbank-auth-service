package com.betterbank.providers;

import com.betterbank.dto.request.LoginRequest;
import com.betterbank.dto.request.RegisterRequest;
import com.betterbank.dto.response.GenericResponse;
import com.betterbank.dto.response.RegistrationOutcome;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


@Slf4j
public class KeycloakAuthProvider implements AuthProvider {
    private static final String DEFAULT_ROLE = "user";

    private final WebClient webClient;

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

    public KeycloakAuthProvider(WebClient.Builder webClientBuilder, Keycloak keycloakAdminClient, AsyncKeycloakTasksService asyncKeycloakTasksService) {
        this.webClient = webClientBuilder.build();
        this.keycloakAdminClient = keycloakAdminClient;
        this.asyncKeycloakTasksService = asyncKeycloakTasksService;
    }


    @Override
    public Mono<RegistrationOutcome> register(RegisterRequest registerRequest) {
        return Mono.fromCallable(() -> {
            UsersResource usersResource = this.keycloakAdminClient.realm(this.keycloakRealm).users();

            if (!usersResource.searchByUsername(registerRequest.getEmail(), true).isEmpty() || !usersResource.searchByEmail(registerRequest.getEmail(), true).isEmpty()) {
                log.error("User with this email is already registered. Try again with a new email.");
                return RegistrationOutcome.USER_EXISTS;
//                throw new IllegalArgumentException("User with this email is already registered. Try again with a new email.");
            }

            // call async method here
//            asyncKeycloakTasksService.createUserInKeycloak(registerRequest);
            log.info("Sending request to AsyncKeycloakTasksService to asynchronously create a user for : {}", registerRequest.getEmail());
            asyncKeycloakTasksService.createUserInKeycloak(registerRequest);


            // return a temp response
            return RegistrationOutcome.INITIATED_ASYNC_PROCESS;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<GenericResponse> login(LoginRequest loginRequest) {
        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakUrl, keycloakRealm);
        log.info("Attempting to login to keycloak at {}", tokenUrl);

        // Prepare form data to send to keycloak
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", keycloakClientId);
        formData.add("client_secret", keycloakClientSecret);
        formData.add("username", loginRequest.getEmail());
        formData.add("password", loginRequest.getPassword());

        return webClient.post().uri(tokenUrl).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(BodyInserters.fromFormData(formData)).retrieve()
                // FIX: Change method reference to lambda for instance method
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> // CORRECTED LINE
                        clientResponse.bodyToMono(JsonNode.class).<Throwable>flatMap(errorJson -> {
                            String error = errorJson.has("error") ? errorJson.get("error").asText() : "unknown_error";
                            String errorDescription = errorJson.has("error_description") ? errorJson.get("error_description").asText() : "No specific error description from Keycloak.";
                            log.warn("Keycloak login client error for {}: Status={}, Error='{}', Description='{}'", loginRequest.getEmail(), clientResponse.statusCode(), error, errorDescription);

                            if ("invalid_grant".equals(error) || "invalid_client".equals(error)) {
                                return Mono.error(new RuntimeException("INVALID_CREDENTIALS"));
                            } else if ("user_not_verified".equals(error) || errorDescription.contains("Email is not verified")) {
                                return Mono.error(new RuntimeException("EMAIL_NOT_VERIFIED"));
                            } else {
                                return Mono.error(new RuntimeException("KEYCLOAK_AUTH_FAILED: " + errorDescription));
                            }
                        }))
                // FIX: Change method reference to lambda for instance method
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> // CORRECTED LINE
                        clientResponse.bodyToMono(String.class).<Throwable>flatMap(errorBody -> {
                            log.error("Keycloak login server error for {}: Status={}, Body={}", loginRequest.getEmail(), clientResponse.statusCode(), errorBody);
                            return Mono.error(new RuntimeException("KEYCLOAK_SERVER_ERROR"));
                        })).bodyToMono(JsonNode.class).map(jsonNode -> {
                    String accessToken = jsonNode.has("access_token") ? jsonNode.get("access_token").asText() : null;
                    String tokenType = jsonNode.has("token_type") ? jsonNode.get("token_type").asText() : null;
                    long expiresIn = jsonNode.has("expires_in") ? jsonNode.get("expires_in").asLong() : 0;

                    String message = String.format("Login successful. Token type: %s, Expires in: %d seconds. (Access token is %s...)", tokenType, expiresIn, accessToken != null && accessToken.length() > 10 ? accessToken.substring(0, 10) : "N/A");
                    log.info("{}", message);
                    return new GenericResponse(message);
                }).onErrorResume(RuntimeException.class, e -> {
                    String message;
                    String status = "FAILED";

                    switch (e.getMessage()) {
                        case "INVALID_CREDENTIALS":
                            message = "Invalid email or password.";
                            break;
                        case "EMAIL_NOT_VERIFIED":
                            message = "Email not verified. Please check your inbox for a verification link.";
                            break;
                        case "KEYCLOAK_SERVER_ERROR":
                            message = "Keycloak server error during login. Please try again later.";
                            break;
                        case "KEYCLOAK_AUTH_FAILED":
                            message = "Authentication failed due to Keycloak error.";
                            break;
                        default:
                            message = "An unexpected error occurred during login. Please contact support.";
                            log.error("Unhandled error during login: {}", e.getMessage(), e);
                            break;
                    }
                    return Mono.just(new GenericResponse(message));
                });
    }
}
