package com.betterbank;

import com.betterbank.controller.AuthController;
import com.betterbank.dto.request.RegisterRequest;
import com.betterbank.dto.response.GenericResponse;
import com.betterbank.dto.response.RegistrationOutcome;
import com.betterbank.service.AuthService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


public class AuthControllerUnitTests {

    // Mock - create a dummy object/bean of the class
    @Mock
    private AuthService mockAuthService;

    // creates a real object/bean of the class and injects the @Mock beans inside
    // its constructor
    @InjectMocks
    private AuthController mockAuthController;

    private RegisterRequest createValidRegisterRequest() {
        return new RegisterRequest("John", "Doe", "johndoe@test.com", "Password123!");
    }


    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void testEndpointShouldReturnSuccess() {
        // When
        ResponseEntity<String> response = mockAuthController.test();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Auth service is working!");
    }

    @Test
    void register_ShouldReturnCreated_WhenInitiatedAsyncProcess() {
        RegisterRequest request = createValidRegisterRequest();
        when(mockAuthService.register(request)).thenReturn(RegistrationOutcome.INITIATED_ASYNC_PROCESS);

        ResponseEntity<GenericResponse> response = mockAuthController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().successStatus()).isTrue();
        assertThat(response.getBody().message()).containsIgnoringCase("Account creation request received");
    }

    @Test
    void register_ShouldReturnOk_WhenUserExists() {
        RegisterRequest request = createValidRegisterRequest();
        when(mockAuthService.register(request)).thenReturn(RegistrationOutcome.USER_EXISTS);

        ResponseEntity<GenericResponse> response = mockAuthController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().successStatus()).isFalse();
        assertThat(response.getBody().message()).containsIgnoringCase("Can't create an account");
    }

    @Test
    void register_ShouldReturnInternalServerError_WhenAuthProviderError() {
        RegisterRequest request = createValidRegisterRequest();
        when(mockAuthService.register(request)).thenReturn(RegistrationOutcome.AUTH_PROVIDER_ERROR);

        ResponseEntity<GenericResponse> response = mockAuthController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().successStatus()).isFalse();
        assertThat(response.getBody().message()).containsIgnoringCase("Server Error");
    }

}
