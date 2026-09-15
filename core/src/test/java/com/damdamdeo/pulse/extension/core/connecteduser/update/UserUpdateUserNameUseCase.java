package com.damdamdeo.pulse.extension.core.connecteduser.update;

import com.damdamdeo.pulse.extension.core.User;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.UserUpdateUsername;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierProvider;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierRepository;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import com.damdamdeo.pulse.extension.core.usecase.audience.Audience;
import com.damdamdeo.pulse.extension.core.usecase.audience.Everyone;

import java.util.List;
import java.util.Objects;

public class UserUpdateUserNameUseCase extends AbstractUpdateUserNameUseCase<UserId, UserUpdateUsername, User> {

    protected UserUpdateUserNameUseCase(final CommandHandler<User, UserId> commandHandler,
                                        final ConnectionIdentifierProvider connectionIdentifierProvider,
                                        final ConnectionIdentifierRepository connectionIdentifierRepository) {
        super(commandHandler, connectionIdentifierProvider, connectionIdentifierRepository);
    }

    @Override
    protected void onUserNameUpdated(final User user, final UserUpdateUsername updateUserNameCommand) throws UseCaseException {
        Objects.requireNonNull(user);
        Objects.requireNonNull(updateUserNameCommand);
    }

    @Override
    public List<Audience> audiences() {
        return List.of(Everyone.INSTANCE);
    }
}
