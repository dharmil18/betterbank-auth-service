package com.betterbank.providers;

import com.betterbank.dto.request.RegisterRequest;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


public class AsyncKeycloakTasksServiceUnitTests {
    @Mock
    private Keycloak mockKeycloakAdminClient;

    private final String keycloakRealm = "test-realm";

    @InjectMocks
    private AsyncKeycloakTasksService asyncKeycloakTasksService;

    @BeforeEach
    void setUp() {
        // Initialize mocks and inject them into the service
        MockitoAnnotations.openMocks(this);

        try {
            Field realmField = AsyncKeycloakTasksService.class.getDeclaredField("keycloakRealm");
            realmField.setAccessible(true);
            realmField.set(asyncKeycloakTasksService, "test-realm");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testAsyncUserCreation() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "johndoe@test.com", "Password123!");
        UsersResource mockUsersResource = mock(UsersResource.class);
        RealmResource mockRealmResource = mock(RealmResource.class);
        Response mockResponse = mock(Response.class);
        URI mockUri = mock(URI.class);
        UserResource mockUserResource = mock(UserResource.class);


        when(mockKeycloakAdminClient.realm(anyString())).thenReturn(mockRealmResource);
        when(mockRealmResource.users()).thenReturn(mockUsersResource);
        when(mockUsersResource.create(any())).thenReturn(mockResponse);
        when(mockResponse.getStatus()).thenReturn(HttpStatus.CREATED.value());
        when(mockResponse.getLocation()).thenReturn(mockUri);
        when(mockUri.getPath()).thenReturn("/auth/admin/realms/{realm}/users/user-id");
        when(mockUsersResource.get(anyString())).thenReturn(mockUserResource);

        // Simulate user creation
        asyncKeycloakTasksService.createUserInKeycloak(request);
        assertThat(mockResponse.getStatus()).isEqualTo(HttpStatus.CREATED.value()); // Assuming 201 Created is the expected response status
        assertThat(mockUri.getPath()).isEqualTo("/auth/admin/realms/{realm}/users/user-id");
        assertThat(mockUserResource).isNotNull();

        // Verify that the user was created with the expected properties
        ArgumentCaptor<UserRepresentation> captor = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(mockUsersResource).create(captor.capture());
        UserRepresentation user = captor.getValue();
        assertThat(user.getUsername()).isEqualTo(request.email());
        assertThat(user.getFirstName()).isEqualTo(request.firstName());
        assertThat(user.getLastName()).isEqualTo(request.lastName());
        assertThat(user.getEmail()).isEqualTo(request.email());
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isEmailVerified()).isFalse();

        // Verify that the user creation method was called
        verify(mockUsersResource, times(1)).create(any(UserRepresentation.class));

        // verify that send email method was called
        verify(mockUserResource, times(1)).sendVerifyEmail();
    }

    @Test
    void testAsyncUserCreationUseError() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "johndoe@test.com", "Password123!");
        UsersResource mockUsersResource = mock(UsersResource.class);
        RealmResource mockRealmResource = mock(RealmResource.class);
        Response mockResponse = mock(Response.class);


        when(mockKeycloakAdminClient.realm(anyString())).thenReturn(mockRealmResource);
        when((mockRealmResource.users())).thenReturn(mockUsersResource);
        when(mockUsersResource.create(any())).thenReturn(mockResponse);
        when(mockResponse.getStatus()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.value());

        // simulate calling the method
        asyncKeycloakTasksService.createUserInKeycloak(request);

        // verify that request object was passed to the createUserInKeycloak method
        ArgumentCaptor<UserRepresentation> captor = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(mockUsersResource).create(captor.capture());
        UserRepresentation user = captor.getValue();
        assertThat(user.getUsername()).isEqualTo(request.email());
        assertThat(user.getFirstName()).isEqualTo(request.firstName());
        assertThat(user.getLastName()).isEqualTo(request.lastName());
        assertThat(user.getEmail()).isEqualTo(request.email());
        assertThat(user.isEnabled()).isTrue();

        // Verify that the user creation method was called
        verify(mockUsersResource, times(1)).create(any(UserRepresentation.class));

        // verify that the response is anything other than 201 Created
        assertThat(mockResponse.getStatus()).isNotEqualTo(HttpStatus.CREATED.value());

    }

}

