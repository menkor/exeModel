package org.exemodel.plugin;

/**
 * @author zp [15951818230@163.com]
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
