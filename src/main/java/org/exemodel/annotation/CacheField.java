package org.exemodel.annotation;

import java.lang.annotation.ElementType;

/**
 * mark require to be cached field
 * example:
 * {@code
 *  @CacheField private int userId;
 * }
 * @author zp [15951818230@163.com]
 */
@java.lang.annotation.Target({ElementType.FIELD, ElementType.METHOD})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface CacheField {
}
