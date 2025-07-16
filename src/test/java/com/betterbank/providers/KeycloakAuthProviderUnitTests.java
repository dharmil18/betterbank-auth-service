package com.betterbank.providers;

import com.betterbank.dto.request.RegisterRequest;
import com.betterbank.dto.response.RegistrationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class KeycloakAuthProviderUnitTests {
    @Mock
    private Keycloak mockKeycloakAdminClient;

    @Mock
    private AsyncKeycloakTasksService mockAsyncKeycloakTasksService;

    @InjectMocks
    private KeycloakAuthProvider keycloakAuthProvider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Set keycloakRealm via reflection
        try {
            Field realmField = KeycloakAuthProvider.class.getDeclaredField("keycloakRealm");
            realmField.setAccessible(true);
            realmField.set(keycloakAuthProvider, "test-realm");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void register_ShouldReturnUserExists_WhenUserAlreadyExists() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "johndoe@test.com", "Password123!");
        UsersResource mockUsersResource = mock(UsersResource.class);
        RealmResource mockRealmResource = mock(RealmResource.class);
        when(mockKeycloakAdminClient.realm(anyString())).thenReturn(mockRealmResource);
        when(mockRealmResource.users()).thenReturn(mockUsersResource);
        // Simulate user exists by username
        when(mockUsersResource.searchByUsername(eq(request.email()), anyBoolean())).thenReturn(List.of(new UserRepresentation()));
        when(mockUsersResource.searchByEmail(eq(request.email()), anyBoolean())).thenReturn(Collections.emptyList());

        RegistrationOutcome outcome = keycloakAuthProvider.register(request);
        assertThat(outcome).isEqualTo(RegistrationOutcome.USER_EXISTS);
        verify(mockUsersResource, times(1)).searchByUsername(eq(request.email()), anyBoolean());
        verify(mockUsersResource, never()).searchByEmail(eq(request.email()), anyBoolean());
    }

    @Test
    void registerShouldReturnInitiatedAsyncProcessWhenUserDoesNotExist() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "johndoe@test.com", "Password123!");
        UsersResource mockUsersResource = mock(UsersResource.class);
        RealmResource mockRealmResource = mock(RealmResource.class);
        when(mockKeycloakAdminClient.realm(anyString())).thenReturn(mockRealmResource);
        when(mockRealmResource.users()).thenReturn(mockUsersResource);

        // Simulate user does not exist by username and email
        when(mockUsersResource.searchByUsername(eq(request.email()), anyBoolean())).thenReturn(Collections.emptyList());
        when(mockUsersResource.searchByEmail(eq(request.email()), anyBoolean())).thenReturn(Collections.emptyList());

        RegistrationOutcome outcome = keycloakAuthProvider.register(request);
        assertThat(outcome).isEqualTo(RegistrationOutcome.INITIATED_ASYNC_PROCESS);
        verify(mockUsersResource, times(1)).searchByUsername(eq(request.email()), anyBoolean());
        verify(mockUsersResource, times(1)).searchByEmail(eq(request.email()), anyBoolean());
        verify(mockAsyncKeycloakTasksService, times(1)).createUserInKeycloak(request);
    }

    @Test
    void registerShouldReturnAuthProviderErrorWhenProcessingException() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "johndoe@test.com", "Password123!");
        RealmResource mockRealmResource = mock(RealmResource.class);
        when(mockKeycloakAdminClient.realm(anyString())).thenReturn(mockRealmResource);
        // Simulate ProcessingException when calling users()
        when(mockRealmResource.users()).thenThrow(new jakarta.ws.rs.ProcessingException("Connection error"));

        RegistrationOutcome outcome = keycloakAuthProvider.register(request);
        assertThat(outcome).isEqualTo(RegistrationOutcome.AUTH_PROVIDER_ERROR);
    }
}
