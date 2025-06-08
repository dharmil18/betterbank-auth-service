package com.betterbank.providers;

import com.betterbank.dto.request.LoginRequest;
import com.betterbank.dto.request.RegisterRequest;
import com.betterbank.dto.response.RegistrationOutcome;
import com.betterbank.service.AsyncKeycloakTasksService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


@Slf4j
public class KeycloakAuthProvider implements AuthProvider {
    private static final String DEFAULT_ROLE = "user";

    private final WebClient webClient;

    private final Keycloak keycloakAdminClient;

    private final AsyncKeycloakTasksService asyncKeycloakTasksService;


    //    @Value("${app.config.keycloak.url}")
//    private String keycloakUrl;
//
    @Value("${app.config.keycloak.realm}")
    private String keycloakRealm;
//
//    @Value("${app.config.keycloak.client_id}")
//    private String keycloakClientId;
//
//    @Value("${app.config.keycloak.client-secret}")
//    private String keycloakClientSecret;
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
    public Mono<JsonNode> login(LoginRequest loginRequest) {
        return null;
    }
}
