package org.exemodel.util;

/**
 * Created by zp on 16/9/13.
 */
public class NumberUtil {
    public  static  int intValue(Object number){
        if(number instanceof Number){
            return ((Number) number).intValue();
        }else{
            return -1;
        }

    }

    public  static  boolean isUndefined(Object number){
        if(number==null|| NumberUtil.intValue(number)==0){
            return true;
        }
        return false;

    }


}
