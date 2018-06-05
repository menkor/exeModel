package org.exemodel.orm;


import org.exemodel.annotation.MethodName;
import org.exemodel.plugin.Transferable;
import org.exemodel.util.StringUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * java bean property(field/get-method/set-method)'s wrapper of java bean
 */
public class FieldAccessor {
    private final static int totalFieldsNum = 1000;
    private Field field;
    private Method getMethod;
    private Method setMethod;
    private final String name;
    private static final Map<Class<?>, Object> primitiveDefaults = new HashMap<Class<?>, Object>();

    static {
        primitiveDefaults.put(Integer.TYPE, Integer.valueOf(0));
        primitiveDefaults.put(Short.TYPE, Short.valueOf((short) 0));
        primitiveDefaults.put(Byte.TYPE, Byte.valueOf((byte) 0));
        primitiveDefaults.put(Float.TYPE, Float.valueOf(0f));
        primitiveDefaults.put(Double.TYPE, Double.valueOf(0d));
        primitiveDefaults.put(Long.TYPE, Long.valueOf(0L));
        primitiveDefaults.put(Boolean.TYPE, Boolean.FALSE);
        primitiveDefaults.put(Character.TYPE, Character.valueOf((char) 0));
    }

    private static final ConcurrentMap<String, FieldAccessor> fieldAccessorCache = new ConcurrentHashMap<>(totalFieldsNum);

    public static FieldAccessor getFieldAccessor(Class<?> cls, String name) {
        StringBuilder stringBuilder = new StringBuilder(cls.getCanonicalName());
        stringBuilder.append("@");
        stringBuilder.append(name);
        String key = stringBuilder.toString();
        FieldAccessor fieldAccessor = fieldAccessorCache.get(key);
        if (fieldAccessor == null) {
            fieldAccessor = new FieldAccessor(cls,name);
            fieldAccessorCache.putIfAbsent(key,fieldAccessor);
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
     * first find on field, then find on get-field-method, then on set-field-method
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
        Object value = get(obj);
        if(value!=null && Transferable.class.isAssignableFrom(field.getType())){
            return ((Transferable) value).to();
        }
        return value;
    }



    public void setProperty(Object obj, Object value) {
        Class<?> type = getPropertyType();
        if (value == null && type.isPrimitive()) {
            value = primitiveDefaults.get(type);
        }
        if(value!= null && type!= value.getClass()&&Transferable.class.isAssignableFrom(type)){
            try {
                Transferable instance = (Transferable) (type.isEnum()?type.getEnumConstants()[0]:type.newInstance());
                set(obj,instance.from(value));
                return;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (this.isCompatibleType(value, type)) {
            set(obj,value);
        }else {
            throw new RuntimeException(String.format("Can't set type %s %s to field  %s type %s",value.getClass(),value,name,type));
        }
    }

    public void setScalarProperty(Object obj, Object value){
        if(!(value instanceof Number)){
            throw new RuntimeException(String.format("Can't find accessor of set property %s", name));
        }
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

    private boolean isCompatibleType(Object value, Class<?> type) {
        // Do object check first, then primitives
        if (value == null || type.isInstance(value) || matchesPrimitive(type, value.getClass())) {
            return true;

        }
        return false;
    }

    /**
     * Check whether a value is of the same primitive type as <code>targetType</code>.
     *
     * @param targetType The primitive type to target.
     * @param valueType  The value to match to the primitive type.
     * @return Whether <code>valueType</code> can be coerced (e.g. autoboxed) into <code>targetType</code>.
     */
    private boolean matchesPrimitive(Class<?> targetType, Class<?> valueType) {
        if (!targetType.isPrimitive()) {
            return false;
        }

        try {
            // see if there is a "TYPE" field.  This is present for primitive wrappers.
            Field typeField = valueType.getField("TYPE");
            Object primitiveValueType = typeField.get(valueType);

            if (targetType == primitiveValueType) {
                return true;
            }
        } catch (NoSuchFieldException|IllegalAccessException e) {
            return false;
        }
        return false;
    }

    private Object get(Object obj) {
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

    private void  set(Object obj, Object value){
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
}