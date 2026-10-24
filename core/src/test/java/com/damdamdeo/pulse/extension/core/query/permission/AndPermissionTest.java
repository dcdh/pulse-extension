package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.Owner;
import com.damdamdeo.pulse.extension.core.OwnerQuery;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import com.damdamdeo.pulse.extension.core.usecase.permission.UserValidity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AndPermissionTest {

    @Mock
    OwnerQuery ownerQuery;

    @Mock
    UserValidity userValidity;

    @Mock
    PermissionExecutionContext permissionExecutionContext;

    CompositePermission<TodoId> USER_IS_OWNER_AND_VALID;

    UserId userId = UserId.USER_1;

    @BeforeEach
    void setup() {
        USER_IS_OWNER_AND_VALID = new IsOwnerPermission(userId, ownerQuery)
                .and(new IsUserValidePermission(userId, userValidity));
    }

    @ParameterizedTest
    @MethodSource("provide")
    void shouldAllowPermissionOnAggregateIdPermissionExecutionContext(final Owner givenOwner, final boolean givenOwnerValidity, final boolean expected) throws QueryException {
        // Given
        doReturn(givenOwner).when(ownerQuery).getByTodoId(TodoId.USER_1_TODO_1);
        doReturn(givenOwnerValidity).when(userValidity).isValid(userId);

        // When
        final boolean isSatisfiedBy = USER_IS_OWNER_AND_VALID.allow(TodoId.USER_1_TODO_1, permissionExecutionContext);

        // Then
        assertThat(isSatisfiedBy).isEqualTo(expected);
    }

    private static Stream<Arguments> provide() {
        return Stream.of(
                Arguments.of(Owner.USER_1, true, true),
                Arguments.of(Owner.USER_1, false, false),
                Arguments.of(new Owner(UserId.USER_2), true, false),
                Arguments.of(new Owner(UserId.USER_2), false, false)
        );
    }
}
