package com.bsbowl.onti.domain.user.service;

import com.bsbowl.onti.domain.user.dto.PasswordChangeRequest;
import com.bsbowl.onti.domain.user.dto.UserProfileUpdateRequest;
import com.bsbowl.onti.domain.user.entity.User;
import com.bsbowl.onti.domain.user.repository.UserRepository;
import com.bsbowl.onti.global.exception.CustomException;
import com.bsbowl.onti.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    @Test
    void updateProfile_appliesOnlyNonNullFields() {
        User user = User.builder().email("a@onti.com").password("x").name("옛이름").build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        var response = userService.updateProfile("user-1", new UserProfileUpdateRequest("새이름", null, "소개글"));

        assertThat(response.name()).isEqualTo("새이름");
        assertThat(response.bio()).isEqualTo("소개글");
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsInvalidCurrentPassword() {
        User user = User.builder().email("a@onti.com").password("encoded-old").name("a").build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword("user-1",
                new PasswordChangeRequest("wrong", "new-password")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CURRENT_PASSWORD);
    }

    @Test
    void changePassword_correctCurrentPassword_encodesAndStoresNewPassword() {
        User user = User.builder().email("a@onti.com").password("encoded-old").name("a").build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");

        userService.changePassword("user-1", new PasswordChangeRequest("old", "new-password"));

        assertThat(user.getPassword()).isEqualTo("encoded-new");
    }

    @Test
    void deleteAccount_deletesUser() {
        User user = User.builder().email("a@onti.com").password("x").name("a").build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        userService.deleteAccount("user-1");

        verify(userRepository).delete(user);
    }
}
