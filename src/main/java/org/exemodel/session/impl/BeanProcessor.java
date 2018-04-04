package org.exemodel.session.impl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.exemodel.exceptions.JdbcRuntimeException;
import org.exemodel.orm.FieldAccessor;
import org.exemodel.orm.ModelMeta;
import org.exemodel.util.StringUtil;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@SuppressWarnings("unchecked")
public class BeanProcessor {
    private static Log logger = LogFactory.getLog(JdbcSession.class);
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
                return (T) processColumn(resultSet,1,type);
            }

            if(java.util.Date.class.isAssignableFrom(type) || type.isEnum() ){
                return (T) convert(resultSet.getObject(1),type);
            }

            T bean = type.newInstance();
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            Map<String, FieldAccessor> accessorMap = ModelMeta.getModelMeta(type).getAccessorMap();
            int columns = resultSetMetaData.getColumnCount();
            for (int i = 0; i < columns; i++) {
                String columnName = resultSetMetaData.getColumnLabel(i+1);
                if (null == columnName || 0 == columnName.length()) {
                    columnName = resultSetMetaData.getColumnName(i+1);
                }
                FieldAccessor accessor = accessorMap.get(StringUtil.underscoreName(columnName));
                if(accessor == null){
                    logger.warn(String.format("Result column %s has selected but not be stored to %s",columnName,type));
                }else{
                    Class<?> columnType = accessor.getPropertyType();
                    Object value = processColumn(resultSet,i+1,columnType);
                    accessor.setProperty(bean, convert(value,columnType));
                }
            }
            return bean;
        } catch (SQLException|InstantiationException|IllegalAccessException e) {
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
        if(propType.equals(InputStream.class)){
            return rs.getBinaryStream(index);
        }
        if(propType.equals(BigDecimal.class)){
            return rs.getBigDecimal(index);
        }
        if(propType.equals(byte[].class)||propType.equals(Byte[].class)){
            return rs.getBytes(index);
        }
        if(propType.equals(BigInteger.class)){
            return  new BigInteger(rs.getString(index));
        }
        if(propType.equals(Array.class)){
            return rs.getArray(index);
        }
        Object value = rs.getObject(index);
        return value;
    }

    protected   boolean isSimpleType(Class<?> type) {
        return (type.isPrimitive() && type != void.class) ||
                type == Double.class || type == Float.class || type == Long.class ||
                type == Integer.class || type == Short.class || type == Character.class ||
                type == Byte.class || type == Boolean.class || type == String.class||
                type==Timestamp.class||type==SQLXML.class||type==InputStream.class||type==BigDecimal.class||
                type==byte[].class||type==Byte[].class||type==BigInteger.class||type==Array.class;
    }




}
