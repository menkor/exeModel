package org.exemodel.plugin;

import java.lang.reflect.Method;

/**
 * Created by xiaofengxu on 18/5/28.
 */
public interface Transferable<S extends Transferable,T> {

    /**
     * convert this instance to target
     */
    T to();

    /**
     *
     * convert target to source object
     */
    S from(T target);

}
