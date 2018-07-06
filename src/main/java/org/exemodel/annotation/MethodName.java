package org.exemodel.annotation;

import java.lang.annotation.ElementType;

/**
 *
 * @author zp [15951818230@163.com]
 */
@java.lang.annotation.Target({ElementType.TYPE,ElementType.FIELD})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface MethodName {
    String get() default "";
    String set() default "";
}
