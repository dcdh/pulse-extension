package com.damdamdeo.pulse.extension.core.query;

// define a priority to avoid unnecessary computing.
// example: a Query has ROLE_RESTRICTED and PARTICIPANT role. ROLE_RESTRICATED is ultra-fast meanwhile PARTICIPANT
// will do a lot of processing so ROLE based must be checked first.
public enum Audience {

    EVERYONE {
        @Override
        Integer priority() {
            return 0;
        }
    },
    ROLE_RESTRICTED {
        @Override
        Integer priority() {
            return 1;
        }
    },
    PARTICIPANT {
        @Override
        Integer priority() {
            return 2;
        }
    };

    abstract Integer priority();
}
