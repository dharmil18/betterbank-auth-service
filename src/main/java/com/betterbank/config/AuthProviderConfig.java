package com.betterbank.config;


import com.betterbank.providers.AuthProvider;
import com.betterbank.providers.KeycloakAuthProvider;
import com.betterbank.providers.AsyncKeycloakTasksService;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AuthProviderConfig {
    private final WebClient.Builder webClientBuilder;

    public AuthProviderConfig(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    // Your existing KeycloakAdminClient bean (assuming it exists or you make it one)
    @Bean
    public Keycloak keycloakAdminClient(
            @Value("${app.config.keycloak.url}") String keycloakUrl,
            @Value("${app.config.keycloak.realm}") String keycloakRealm,
            @Value("${app.config.keycloak.admin.client-id}") String keycloakAdminClientId,
            @Value("${app.config.keycloak.admin.client-secret}") String keycloakAdminClientSecret,
            @Value("${app.config.keycloak.admin.username}") String keycloakAdminUsername,
            @Value("${app.config.keycloak.admin.password}") String keycloakAdminPassword) {

        return org.keycloak.admin.client.KeycloakBuilder.builder()
                .serverUrl(keycloakUrl)
                .realm(keycloakRealm)
                .clientId(keycloakAdminClientId)
                .clientSecret(keycloakAdminClientSecret)
                .username(keycloakAdminUsername)
                .password(keycloakAdminPassword)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.config.auth.provider.type", havingValue = "keycloak", matchIfMissing = true)
    public AuthProvider keycloakAuthProvider(WebClient.Builder webClientBuilder, Keycloak keycloakAdminClient, AsyncKeycloakTasksService asyncKeycloakTasksService) {
        return new KeycloakAuthProvider(webClientBuilder, keycloakAdminClient, asyncKeycloakTasksService);
    }

    // Create a new bean if provider changes to lets say AWS Cognito
}
