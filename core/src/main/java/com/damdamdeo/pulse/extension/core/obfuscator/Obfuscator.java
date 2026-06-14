package com.damdamdeo.pulse.extension.core.obfuscator;

public interface Obfuscator {

    String obfuscate(String value) throws UnableToObfuscateException;

    String deObfuscate(String obfuscated) throws UnableToDeObfuscateException, UnknownObfuscatedException;
}
