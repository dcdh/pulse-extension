package com.damdamdeo.pulse.extension.obfuscator.runtime.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@JacksonAnnotationsInside
public @interface Obfuscate {
}
