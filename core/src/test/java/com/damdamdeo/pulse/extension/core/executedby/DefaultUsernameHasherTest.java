package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

class DefaultUsernameHasherTest {

    @Test
    void shouldHashUsername() {
        // Given
        final Hasher hasher = mock(Hasher.class);
        final DefaultUsernameHasher usernameHasher = new DefaultUsernameHasher(hasher);
        final Username username = new Username("bob@mail.com");

        when(hasher.hash("bob@mail.com")).thenReturn("hashed-username");

        // When
        final UsernameHashed result = usernameHasher.hash(username);

        // Then
        assertAll(
                () -> assertThat(result).isEqualTo(new UsernameHashed("hashed-username")),
                () -> verify(hasher).hash(anyString()),
                () -> verifyNoMoreInteractions(hasher)
        );
    }
}
