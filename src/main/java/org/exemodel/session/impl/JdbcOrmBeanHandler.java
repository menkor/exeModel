package org.exemodel.session.impl;
import org.exemodel.orm.ModelMeta;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.RowProcessor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


public class JdbcOrmBeanHandler<T> implements ResultSetHandler<T> {
    private final Class<T> type;
    private final RowProcessor convert;
    private static final Map<Class<?>, RowProcessor> ROW_PROCESSOR_MAP = new HashMap<Class<?>, RowProcessor>();
    public static synchronized RowProcessor getRowProcessor(Class<?> modelCls, ModelMeta modelMeta) {
        if(ROW_PROCESSOR_MAP.containsKey(modelCls)) {
            return ROW_PROCESSOR_MAP.get(modelCls);
        }
        RowProcessor rowProcessor = new BasicRowProcessor(new JdbcOrmBeanProcessor(modelCls, modelMeta));
        ROW_PROCESSOR_MAP.put(modelCls, rowProcessor);
        return rowProcessor;
    }

    public static boolean isRawType(Class<?> type) {
        if(type==null) {
            return false;
        }
        return type == String.class || type == Integer.class || type == Long.class || type == Float.class || type == Double.class
                || type == Boolean.class || "int".equals(type.getName()) || "long".equals(type.getName())
                || "boolean".equals(type.getName()) || "float".equals(type.getName()) || "double".equals(type.getName());
    }

    public static Object getResultSetRawOfRawType(ResultSet rs, Class<?> type) throws SQLException{
        if(type==null||!isRawType(type)) {
            return null;
        }
        try {
            if (type == String.class) {
                return rs.getString(1);
            }
            if(type == Integer.class || "int".equals(type.getName())) {
                return rs.getInt(1);
            }
            if(type == Long.class || "long".equals(type.getName())) {
                return rs.getLong(1);
            }
            if(type == Float.class || "float".equals(type.getName())) {
                return rs.getFloat(1);
            }
            if(type==Boolean.class || "boolean".equals(type.getName())) {
                return rs.getBoolean(1);
            }
            if(type==Double.class || "double".equals(type.getName())) {
                return rs.getDouble(1);
            }
            // TODO: support array/json
            return null;
        }catch (SQLException e) {
            throw e;
        }
    }

    public JdbcOrmBeanHandler(Class<T> type, ModelMeta modelMeta) {
        this(type, getRowProcessor(type, modelMeta));
    }

    public JdbcOrmBeanHandler(Class<T> type, RowProcessor convert) {
        this.type = type;
        this.convert = convert;
    }

    public T handle(ResultSet rs) throws SQLException {
        if(!rs.next()) {
            return null;
        }
        if(isRawType(type)) {
            return (T)getResultSetRawOfRawType(rs, type);
        }
        return this.convert.toBean(rs, this.type);
    }
}
