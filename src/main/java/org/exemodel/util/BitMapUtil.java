package org.exemodel.util;
import java.lang.reflect.Field;
import java.util.*;

/**
 * @author zp
 */

public class BitMapUtil {

    public static <T> Integer intBitMap(T dto) {
        if(dto==null){
            return null;
        }

        int i = 1, res = 0;
        for (Field field : getSortedFields(dto.getClass())) {
            field.setAccessible(true);
            try {
                boolean rs = (boolean) field.get(dto);
                if (rs) {
                    res = res + i;
                }
            } catch (IllegalAccessException e) {
               throw new RuntimeException(e);
            }
            i*=2;
        }
        return res;
    }

    public static <T>  T  fillDTO(int bitMap, Class<T> clazz) {
        try {
            T dto = clazz.newInstance();
            int i = 1;
            for(Field field:getSortedFields(clazz)){
                field.setAccessible(true);
                field.set(dto,(bitMap&i)!=0);
                i*=2;
            }
            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //keep order
    private  static Field[] getSortedFields(Class clazz){
        Field[]  fieldArray = clazz.getDeclaredFields();

        Arrays.sort(fieldArray, new Comparator<Field>() {
            @Override
            public int compare(Field o1, Field o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        return  fieldArray;

    }

}
