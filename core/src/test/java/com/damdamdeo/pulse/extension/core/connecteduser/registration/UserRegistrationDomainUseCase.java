package com.damdamdeo.pulse.extension.core.connecteduser.registration;

import com.damdamdeo.pulse.extension.core.SequenceNumber;
import com.damdamdeo.pulse.extension.core.User;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.RegisterUser;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierProvider;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierRepository;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import com.damdamdeo.pulse.extension.core.usecase.audience.Audience;
import com.damdamdeo.pulse.extension.core.usecase.audience.Everyone;

import java.util.List;
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
    protected void onUserNameRegistered(final User user, final RegisterUser registrationCommand) throws UseCaseException {
        Objects.requireNonNull(user);
        Objects.requireNonNull(registrationCommand);
    }

    @Override
    public List<Audience> audiences() {
        return List.of(Everyone.INSTANCE);
    }
}
