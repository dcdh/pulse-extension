package com.damdamdeo.pulse.extension.core.connecteduser.registration;

import com.damdamdeo.pulse.extension.core.BusinessException;
import com.damdamdeo.pulse.extension.core.SequenceNumber;
import com.damdamdeo.pulse.extension.core.User;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.RegisterUser;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierProvider;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierRepository;

import java.util.Objects;

public class UserRegistrationDomainUseCase extends AbstractRegistrationDomainUseCase<UserId, RegisterUser, User> {

    protected UserRegistrationDomainUseCase(final CommandHandler<User, UserId> commandHandler,
                                            final ConnectionIdentifierProvider connectionIdentifierProvider,
                                            final ConnectionIdentifierRepository connectionIdentifierRepository) {
        super(commandHandler, connectionIdentifierProvider, connectionIdentifierRepository);
    }

    @Override
    protected UserId from(final SequenceNumber sequenceNumber) {
        return new UserId(sequenceNumber);
    }

    @Override
    protected void onUserNameRegistered(final User user, final RegisterUser registrationCommand) throws BusinessException {
        Objects.requireNonNull(user);
        Objects.requireNonNull(registrationCommand);
    }
}
