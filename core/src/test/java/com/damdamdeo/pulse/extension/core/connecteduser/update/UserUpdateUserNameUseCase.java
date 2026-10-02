package com.damdamdeo.pulse.extension.core.connecteduser.update;

import com.damdamdeo.pulse.extension.core.User;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.Handled;
import com.damdamdeo.pulse.extension.core.command.UserUpdateUsername;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierProvider;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierRepository;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import com.damdamdeo.pulse.extension.core.usecase.permission.Everyone;
import com.damdamdeo.pulse.extension.core.usecase.permission.Permission;

import java.util.List;
import java.util.Objects;

public class UserUpdateUserNameUseCase extends AbstractUpdateUserNameUseCase<UserId, UserUpdateUsername, User> {

    protected UserUpdateUserNameUseCase(final CommandHandler<User, UserId> commandHandler,
                                        final ConnectionIdentifierProvider connectionIdentifierProvider,
                                        final ConnectionIdentifierRepository connectionIdentifierRepository) {
        super(commandHandler, connectionIdentifierProvider, connectionIdentifierRepository);
    }

    @Override
    protected void onUserNameUpdated(final Handled<User, UserId> handled, final UserUpdateUsername updateUserNameCommand) throws UseCaseException {
        Objects.requireNonNull(handled);
        Objects.requireNonNull(updateUserNameCommand);
    }

    @Override
    public List<Permission<UserId, UserUpdateUsername>> permissions() {
        return List.of(new Everyone<>());
    }
}
