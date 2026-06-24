package com.damdamdeo.pulse.extension.core.connecteduser.update;

import com.damdamdeo.pulse.extension.core.BusinessException;
import com.damdamdeo.pulse.extension.core.User;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.UserUpdateUsername;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierProvider;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierRepository;

import java.util.Objects;

public class UserUpdateUserNameUseCase extends AbstractUpdateUserNameUseCase<UserId, UserUpdateUsername, User> {

    protected UserUpdateUserNameUseCase(final CommandHandler<User, UserId> commandHandler,
                                        final ConnectionIdentifierProvider connectionIdentifierProvider,
                                        final ConnectionIdentifierRepository connectionIdentifierRepository) {
        super(commandHandler, connectionIdentifierProvider, connectionIdentifierRepository);
    }

    @Override
    protected void onUserNameUpdated(final User user, final UserUpdateUsername updateUserNameCommand) throws BusinessException {
        Objects.requireNonNull(user);
        Objects.requireNonNull(updateUserNameCommand);
    }
}
