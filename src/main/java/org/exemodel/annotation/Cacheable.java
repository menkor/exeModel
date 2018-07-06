package org.exemodel.annotation;
import java.lang.annotation.ElementType;

/**
 * cache model annotation
 * @author zp [15951818230@163.com]
 */
@java.lang.annotation.Target({ElementType.TYPE})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Cacheable  {
    // entity short key
    String key() default "";
    // whether cache all fields
    boolean all() default false;
}
