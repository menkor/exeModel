package org.exemodel.util;

/**
 * @author zp [15951818230@163.com]
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
