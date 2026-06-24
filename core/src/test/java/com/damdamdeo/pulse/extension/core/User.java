package com.damdamdeo.pulse.extension.core;

import com.damdamdeo.pulse.extension.core.command.RegisterUser;
import com.damdamdeo.pulse.extension.core.command.UserUpdateUsername;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.event.EventAppender;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.event.UserRegistered;
import com.damdamdeo.pulse.extension.core.event.UsernameUpdated;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;

import java.util.Objects;

public class User extends AggregateRoot<UserId> {

    private Username username;

    public User(final UserId id) {
        super(id);
    }

    public void handle(final RegisterUser registerUser, final ExecutionContext executionContext, final EventAppender<UserId> eventAppender) throws BusinessException {
        Objects.requireNonNull(registerUser);
        Objects.requireNonNull(executionContext);
        Objects.requireNonNull(eventAppender);
        eventAppender.append(new UserRegistered(executionContext.executedBy().username()));
    }

    public void handle(final UserUpdateUsername userUpdateUserName, final ExecutionContext executionContext, final EventAppender<UserId> eventAppender) throws BusinessException {
        Objects.requireNonNull(userUpdateUserName);
        Objects.requireNonNull(executionContext);
        Objects.requireNonNull(eventAppender);
        eventAppender.append(new UsernameUpdated(executionContext.executedBy().username()));
    }

    public void on(final UserRegistered userRegistered, final ExecutedBy executedBy) {
        Objects.requireNonNull(userRegistered);
        Objects.requireNonNull(executedBy);
        this.username = userRegistered.username();
    }

    public void on(final UsernameUpdated usernameUpdated, final ExecutedBy executedBy) {
        Objects.requireNonNull(usernameUpdated);
        Objects.requireNonNull(executedBy);
        this.username = usernameUpdated.username();
    }

    public Username username() {
        return username;
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
