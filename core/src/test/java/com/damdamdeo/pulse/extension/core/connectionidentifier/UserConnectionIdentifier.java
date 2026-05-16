package com.damdamdeo.pulse.extension.core.connectionidentifier;

public record UserConnectionIdentifier() implements ConnectionIdentifier {

    @Override
    public String id() {
        return "damien.clementdhuart@gmail.com";
    }
}
