package io.alv.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@SuppressWarnings({"unused"})
public @interface Handler {

  Class<?>[] broadcast();

  Class<?>[] unicast() default {Void.class};

}
