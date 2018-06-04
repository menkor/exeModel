package org.exemodel.component;

import org.exemodel.plugin.Transferable;

/**
 * Created by xiaofengxu on 18/5/28.
 */
public enum Gender implements Transferable<Gender,Integer>{
    MAN(0,"男人"),FEMALE(1,"女人"),SECRET(2,"保密");

    private int index;
    private String name;

    Gender(int index,String name) {
        this.index = index;
        this.name = name;
    }

    @Override
    public Integer to() {
        return this.index;
    }

    @Override
    public  Gender from(Integer des) {
        for(Gender gender:values()){
           if(gender.to() == des){
               return gender;
           }
        }
        throw new RuntimeException("Can't recognition gender " + des);
    }


    public String getName() {
        return name;
    }
}
