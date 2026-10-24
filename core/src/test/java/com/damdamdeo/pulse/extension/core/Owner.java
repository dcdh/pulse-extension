package com.damdamdeo.pulse.extension.core;

import java.util.Objects;

public record Owner(UserId id) {

    public static final Owner USER_1 = new Owner(UserId.USER_1);

    public Owner {
        Objects.requireNonNull(id);
    }
}
