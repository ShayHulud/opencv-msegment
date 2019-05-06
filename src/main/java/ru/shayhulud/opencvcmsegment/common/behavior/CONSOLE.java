package ru.shayhulud.opencvcmsegment.common.behavior;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Обозначение методов, предназначенных для консольного.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD})
public @interface CONSOLE {
}
