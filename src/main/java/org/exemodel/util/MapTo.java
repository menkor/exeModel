package org.exemodel.util;

/**
 * Created by zp on 17/4/11.
 */
public interface MapTo<T,E> {
    T apply(E t);
}
