package com.damdamdeo.pulse.extension.hasher.runtime;

import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import org.junit.jupiter.params.provider.Arguments;

import java.util.Map;
import java.util.stream.Stream;

public class HasherProvider {

    public static final Map<OwnedBy, Hash<OwnedBy>> GIVEN_OWNED_BY_EXPECTED_HASH = Map.of(
            Todo.OWNED_BY_USER_1, new Hash<>("825262468b4cb777358139eafbdec2e0477f898202d8cab60ae9c3a8e79a0de9"),
            Todo.OWNED_BY_USER_2, new Hash<>("2815ed328276952fafaee264dab3acb68142aff118be1992561e306412baa0ca"),
            Todo.OWNED_BY_USER_3, new Hash<>("c5d57f3cc9009706a027b97aa34a0487da68b019f7b910c92b7d01834c405a0c"));

    private HasherProvider() {
    }

    public static Stream<Arguments> provideUserHash() {
        return GIVEN_OWNED_BY_EXPECTED_HASH.entrySet().stream()
                .map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
    }
}
