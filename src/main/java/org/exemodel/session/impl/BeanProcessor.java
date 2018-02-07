package org.exemodel.session.impl;

import org.exemodel.exceptions.JdbcRuntimeException;
import org.exemodel.orm.FieldAccessor;
import org.exemodel.orm.ModelMeta;

import java.math.BigInteger;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class BeanProcessor {

    public <T> T toBean(ResultSet resultSet, Class<? extends T> type) {
        try {
            if(!resultSet.next()){
                return null;
            }
            return createBean(resultSet,type);
        }catch (SQLException e){
            throw new JdbcRuntimeException(e);
        }
    }

    public <T> List<T> toBeanList(ResultSet rs, Class<? extends T> type) {
        List<T> results = new ArrayList<T>();
        try {
            if (!rs.next()) {
                return results;
            }
            do {
                results.add(createBean(rs, type));
            } while (rs.next());
            return results;

        } catch (SQLException e) {
            throw new JdbcRuntimeException(e);
        }
    }

    protected   boolean isSimpleType(Class<?> type) {
        return (type.isPrimitive() && type != void.class) ||
                type == Double.class || type == Float.class || type == Long.class ||
                type == Integer.class || type == Short.class || type == Character.class ||
                type == Byte.class || type == Boolean.class || type == String.class;
    }


    protected Object convert(Object value,Class<?> type){
        if(value instanceof java.util.Date){
            String e = type.getName();
            if ("java.sql.Date".equals(e)) {
                value = new java.sql.Date(((java.util.Date) value).getTime());
            } else if ("java.sql.Time".equals(e)) {
                value = new Time(((java.util.Date) value).getTime());
            } else if ("java.sql.Timestamp".equals(e)) {
                Timestamp tsValue = (Timestamp) value;
                int nanos = tsValue.getNanos();
                value = new Timestamp(tsValue.getTime());
                ((Timestamp) value).setNanos(nanos);
            }
        }else if (value instanceof String && type.isEnum()) {
            value = Enum.valueOf(type.asSubclass(Enum.class), (String) value);
        }
        return value;
    }


    protected <T> T createBean(ResultSet resultSet, Class<? extends T> type){
        try {
            if(isSimpleType(type)){
                return (T) resultSet.getObject(1);
            }

            if(java.util.Date.class.isAssignableFrom(type) || type.isEnum() ){
                return (T) convert(resultSet.getObject(1),type);
            }

            T bean = type.newInstance();
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            Map<String, FieldAccessor> accessorMap = ModelMeta.getModelMeta(type).getAccessorMap();
            int columns = resultSetMetaData.getColumnCount();
            for (int i = 0; i < columns; i++) {
                FieldAccessor accessor = accessorMap.get(resultSetMetaData.getColumnName(i+1));
                Class<?> columnType = accessor.getPropertyType();
                Object value = processColumn(resultSet,i+1,columnType);
                accessor.setProperty(bean, convert(value,columnType));
            }
            return bean;
        } catch (Exception e) {
            throw new JdbcRuntimeException(
                    "Cannot create " + type.getName() + ": " + e.getMessage());
        }
    }

    protected Object processColumn(ResultSet rs, int index, Class<?> propType) throws SQLException {
        if (!propType.isPrimitive() && rs.getObject(index) == null) {
            return null;
        }
        if (propType.equals(String.class)) {
            return rs.getString(index);
        }
        if (propType.equals(Integer.TYPE) || propType.equals(Integer.class)) {
            return rs.getInt(index);
        }
        if (propType.equals(Boolean.TYPE) || propType.equals(Boolean.class)) {
            return rs.getBoolean(index);
        }
        if (propType.equals(Long.TYPE) || propType.equals(Long.class)) {
            return rs.getLong(index);
        }
        if (propType.equals(Double.TYPE) || propType.equals(Double.class)) {
            return rs.getDouble(index);
        }
        if (propType.equals(Float.TYPE) || propType.equals(Float.class)) {
            return rs.getFloat(index);
        }
        if (propType.equals(Short.TYPE) || propType.equals(Short.class)) {
            return rs.getShort(index);
        }
        if (propType.equals(Byte.TYPE) || propType.equals(Byte.class)) {
            return rs.getByte(index);
        }
        if (propType.equals(Timestamp.class)) {
            return rs.getTimestamp(index);
        }
        if (propType.equals(SQLXML.class)) {
            return rs.getSQLXML(index);
        }
        Object value = rs.getObject(index);
        return value;
    }




}
