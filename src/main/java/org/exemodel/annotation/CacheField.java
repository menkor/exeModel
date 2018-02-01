package org.exemodel.annotation;

import java.lang.annotation.ElementType;

/**
 * Created by zp on 17/1/10.
 */
@java.lang.annotation.Target({ElementType.FIELD, ElementType.METHOD})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface CacheField {
}
