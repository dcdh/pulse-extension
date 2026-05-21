package com.damdamdeo.pulse.extension.core;

import com.damdamdeo.pulse.extension.core.command.RegisterUser;
import com.damdamdeo.pulse.extension.core.event.EventAppender;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.event.UserRegistered;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;

import java.util.Objects;

public class User extends AggregateRoot<UserId> {

    public User(final UserId id) {
        super(id);
    }

    public void handle(final RegisterUser registerUser, final ExecutionContext executionContext, final EventAppender<UserId> eventAppender) throws BusinessException {
        Objects.requireNonNull(registerUser);
        Objects.requireNonNull(executionContext);
        Objects.requireNonNull(eventAppender);
        eventAppender.append(new UserRegistered());
    }

    public void on(final UserRegistered userRegistered, final ExecutedBy executedBy) {
        Objects.requireNonNull(userRegistered);
        Objects.requireNonNull(executedBy);
    }

    @Override
    public BelongsTo belongsTo() {
        return BelongsTo.himself(this);
    }

    @Override
    public OwnedBy ownedBy() {
        return OwnedBy.himself(this);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                '}';
    }
}
