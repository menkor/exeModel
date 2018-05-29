package org.exemodel.component;

import org.exemodel.plugin.Transferable;

/**
 * Created by xiaofengxu on 18/5/28.
 */
public enum Gender implements Transferable<Gender,Integer>{
    MAN,FEMALE,SECRET;

    @Override
    public Integer to() {
        return this.ordinal();
    }

    @Override
    public  Gender from(Integer des) {
        return  values()[des];
    }



}
