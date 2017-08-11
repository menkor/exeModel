package org.exemodel.annotation;

import java.lang.annotation.ElementType;

/**
 * Created by zp on 16/9/9.
 */

@java.lang.annotation.Target({ElementType.FIELD, ElementType.METHOD})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface PartitionId {
}
