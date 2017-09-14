package org.exemodel.util;

import java.util.HashMap;
import java.util.Map;

public class PrimitivesWrapper {

    public static Map<Class<?>, Class<?>> get() {
        Map<Class<?>, Class<?>> hashMap = new HashMap<>();
        hashMap.put(boolean.class, Boolean.class);
        hashMap.put(byte.class, Byte.class);
        hashMap.put(char.class, Character.class);
        hashMap.put(double.class, Double.class);
        hashMap.put(float.class, Float.class);
        hashMap.put(int.class, Integer.class);
        hashMap.put(long.class, Long.class);
        hashMap.put(short.class, Short.class);
        hashMap.put(void.class, Void.class);
        return hashMap;
    }
}
