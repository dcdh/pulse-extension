package com.damdamdeo.pulse.extension.query.runtime.file;

public enum ContentDisposition {

    INLINE {
        @Override
        String value() {
            return "inline";
        }
    }, ATTACHMENT {
        @Override
        String value() {
            return "attachment";
        }
    };

    abstract String value();

}
