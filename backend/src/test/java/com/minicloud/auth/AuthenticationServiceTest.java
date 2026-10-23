package com.minicloud.auth;

import com.minicloud.audit.service.AuditService;
import com.minicloud.auth.dto.LoginRequest;
import com.minicloud.auth.dto.LoginResponse;
import com.minicloud.auth.entity.Role;
import com.minicloud.auth.entity.User;
import com.minicloud.auth.repository.UserRepository;
import com.minicloud.auth.service.AuthenticationService;
import com.minicloud.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuditService auditService;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(authenticationManager, userRepository, jwtService, auditService);
    }

    @Test
    void loginReturnsJwtAndAuditsSuccess() {
        User user = new User();
        user.setUsername("admin");
        user.setDisplayName("Admin");
        user.setRole(Role.ADMIN);
        user.setEnabled(true);
        setUserId(user);

        Authentication authentication = new UsernamePasswordAuthenticationToken("admin", "secret");
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(jwtService.createToken("admin", Role.ADMIN)).thenReturn("jwt-token");
        when(jwtService.expirationMs()).thenReturn(1000L);

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("secret");

        LoginResponse response = authenticationService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().username()).isEqualTo("admin");
        assertThat(response.user().role()).isEqualTo(Role.ADMIN);
        verify(auditService).record(eq("admin"), eq("USER_LOGIN"), eq("USER"), any(), eq("SUCCESS"), any());
    }

    private void setUserId(User user) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, UUID.randomUUID());
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
