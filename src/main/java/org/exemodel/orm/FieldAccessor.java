package org.exemodel.orm;


import org.exemodel.annotation.MethodName;
import org.exemodel.util.StringUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * java bean property(field/get-method/set-method)'s wrapper of java bean
 */
public class FieldAccessor {
    private final static int totalFieldsNum = 1000;
    private Field field;
    private Method getMethod;
    private Method setMethod;
    private final String name;

    private static final Map<String, FieldAccessor> fieldAccessorCache = new ConcurrentHashMap<>(totalFieldsNum);

    public static FieldAccessor getFieldAccessor(Class<?> cls, String name) {
        StringBuilder stringBuilder = new StringBuilder(cls.getCanonicalName());
        stringBuilder.append("@");
        stringBuilder.append(name);
        String key = stringBuilder.toString();
        FieldAccessor fieldAccessor = fieldAccessorCache.get(key);
        if (fieldAccessor == null) {
            synchronized (fieldAccessorCache) {
                if (!fieldAccessorCache.containsKey(key)) {
                    fieldAccessor = new FieldAccessor(cls, name);
                    fieldAccessorCache.put(key, fieldAccessor);
                }
            }
        }
        return fieldAccessor;
    }

    @SuppressWarnings("unchecked")
    public FieldAccessor(Class<?> cls, String name) {
        this.name = name;
        try {
            field = cls.getDeclaredField(name);
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            field = null;
        }
        try {
            getMethod = cls.getMethod(getGetMethodName());
        } catch (NoSuchMethodException e) {
            getMethod = null;
        }
        try {
            setMethod = cls.getMethod(getSetMethodName(), field.getType());
        } catch (NoSuchMethodException e) {
            setMethod = null;
        }
    }

    public Class getPropertyType() {
        if (getMethod != null) {
            return getMethod.getReturnType();
        }
        if (field != null) {
            return field.getType();
        } else {
            return null;
        }
    }

    /**
     * get cn.superid.constantapi.annotation of this property, first find on field, then find on get-field-method, then on set-field-method
     *
     * @param annoCls
     * @param <T>
     * @return
     */
    public <T extends Annotation> T getPropertyAnnotation(Class<T> annoCls) {
        if (field != null) {
            T fieldAnnotation = field.getAnnotation(annoCls);
            if (fieldAnnotation != null) {
                return fieldAnnotation;
            }
        }
        if (getMethod != null) {
            T getMethodAnnotation = getMethod.getAnnotation(annoCls);
            if (getMethodAnnotation != null) {
                return getMethodAnnotation;
            }
        }
        if (setMethod != null) {
            return setMethod.getAnnotation(annoCls);
        }
        return null;
    }

    public Object getProperty(Object obj) {
        if (getMethod != null) {
            try {
                return getMethod.invoke(obj);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (field != null) {
            try {
                return field.get(obj);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException(String.format("Can't find accessor of get property value of %s", name));
    }

    public void setProperty(Object obj, Object value) {
        if ((getPropertyType() == int.class || getPropertyType() == Integer.class) && value instanceof Long) {
            value = ((Long) value).intValue();
        }

        if (setMethod != null) {
            try {
                setMethod.invoke(obj, value);
                return;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (field != null) {
            try {
                field.set(obj, value);
                return;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException(String.format("Can't find accessor of set property %s", name));
    }

    private String upperFirstChar(String str) {
        if (str == null) {
            return null;
        }
        String s = str.trim();
        if (s.length() < 1) {
            return s;
        }
        return String.valueOf(s.charAt(0)).toUpperCase() + s.substring(1);
    }

    /**
     * getter method of the property's name
     *
     * @return
     */
    private String getGetMethodName() {
        assert name != null;
        MethodName methodName = field.getAnnotation(MethodName.class);
        if (methodName != null && StringUtil.notEmpty(methodName.get())) {
            return methodName.get();
        }
        if (field.getType() == Boolean.class || "boolean".equals(field.getType().getName())) {
            if (name.indexOf("is") == 0) {
                return name;
            }
            return "is" + upperFirstChar(name);
        }
        return "get" + upperFirstChar(name);
    }

    /**
     * setter method of the property's name
     *
     * @return
     */
    private String getSetMethodName() {
        assert name != null;
        MethodName method = field.getAnnotation(MethodName.class);
        if (method != null && StringUtil.notEmpty(method.set())) {
            return method.set();
        }
        String methodName = name;
        if (field.getType() == Boolean.class || "boolean".equals(field.getType().getName())) {
            if (name.indexOf("is") == 0) {
                methodName = name.substring(2);
            }
        }
        return "set" + upperFirstChar(methodName);
    }
}