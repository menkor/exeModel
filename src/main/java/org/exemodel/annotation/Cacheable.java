package org.exemodel.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;

/**
 * Created by zp on 16/9/6.
 */
@java.lang.annotation.Target({ElementType.TYPE})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Cacheable  {
    String key() default "";
    boolean all() default false;
}
