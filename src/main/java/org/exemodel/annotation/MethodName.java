package org.exemodel.annotation;

import java.lang.annotation.ElementType;

/**
 * Created by zp on 17/4/25.
 */
@java.lang.annotation.Target({ElementType.TYPE,ElementType.FIELD})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface MethodName {
    String get() default "";
    String set() default "";
}
