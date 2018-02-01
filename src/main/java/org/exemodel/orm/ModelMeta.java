package org.exemodel.orm;

import org.exemodel.annotation.CacheField;
import org.exemodel.annotation.Cacheable;
import org.exemodel.annotation.PartitionId;
import org.exemodel.exceptions.JdbcRuntimeException;
import org.exemodel.util.BinaryUtil;
import org.exemodel.util.StringUtil;

import javax.persistence.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModelMeta {
    private final static int fieldsNum = 30;
    private final static int cachedClassNum = 200;
    private Class<?> modelCls;
    private String tableName;
    private String tableSchema;
    private String insertSql;
    private String updateSql;
    private String deleteSql;
    private String findByIdSql;
    private String findByPartitionIdSql;
    private boolean cacheable = false;
    private byte[] key = null;
    private List<ModelColumnMeta> columnMetaList;
    private ModelColumnMeta idColumnMeta;
    private ModelColumnMeta partitionColumn;
    private List<String> cacheColumnList;//{index:fieldName}

    /**
     * column info of orm model class, ignore all fieldNameBytes with @javax.sql.Transient
     */
    public static class ModelColumnMeta {
        public boolean isId = false;
        public boolean isPartition = false;
        public String fieldName;
        public String columnName;
        public Class<?> fieldType;
        public byte[] cacheOrder;//binaryIndex
        public FieldAccessor fieldAccessor;
    }

    private static List<String> registeredKeys = new ArrayList<>();

    /**
     * init column meta and cache it
     *
     * @return
     */
    private List<ModelColumnMeta> getColumnMetaList() {

        Field[] fields = modelCls.getDeclaredFields();
        List<ModelColumnMeta> columnMetas = new ArrayList<>(fieldsNum);
        for (Field field : fields) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                continue;
            }
            FieldAccessor fieldAccessor = new FieldAccessor(modelCls, field.getName());
            if (fieldAccessor.getPropertyAnnotation(Transient.class) != null) {
                continue;
            }
            ModelColumnMeta columnMeta = new ModelColumnMeta();
            columnMeta.fieldAccessor = new FieldAccessor(modelCls, field.getName());
            columnMeta.fieldName = field.getName();
            columnMeta.fieldType = field.getType();

            javax.persistence.Column columnAnno = fieldAccessor.getPropertyAnnotation(javax.persistence.Column.class);
            if (columnAnno == null) {
                columnMeta.columnName = StringUtil.underscoreName(field.getName());
            } else {
                if (StringUtil.isEmpty(columnAnno.name())) {
                    columnMeta.columnName = StringUtil.underscoreName(field.getName());
                } else {
                    columnMeta.columnName = columnAnno.name();
                }
            }

            if (fieldAccessor.getPropertyAnnotation(javax.persistence.Id.class) != null) {
                columnMeta.isId = true;
                this.idColumnMeta = columnMeta;
            } else if (fieldAccessor.getPropertyAnnotation(PartitionId.class) != null) {
                columnMeta.isPartition = true;
                this.partitionColumn = columnMeta;
            }

            columnMetas.add(columnMeta);
        }


        this.columnMetaList = columnMetas;
        return columnMetas;
    }


    public String getInsertSql() {
        if (this.insertSql == null) {
            initInsertSql();
        }
        return this.insertSql;
    }

    private synchronized void initInsertSql() {//init insert sql
        boolean first = true;
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(this.getTableName());
        sql.append(" (");

        for (ModelColumnMeta columnMeta : columnMetaList) {
            if (first) {
                first = false;
            } else {
                sql.append(",");
            }
            sql.append(columnMeta.columnName);
        }

        sql.append(")");
        sql.append(" VALUES (");
        int size = this.getColumnMetaSet().size();
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                sql.append(",");
            }
            sql.append('?');
        }
        sql.append(")");
        this.insertSql = sql.toString();
    }

    public String getUpdateSql() {
        if (this.updateSql == null) {
            initUpdateSql();
        }
        return updateSql;
    }

    private synchronized void initUpdateSql() {
        boolean first = true;
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(this.getTableName());
        sql.append(" SET ");
        for (ModelColumnMeta columnMeta : columnMetaList) {
            if (!columnMeta.isId && !columnMeta.isPartition) {// id and partitionId can't set
                if (first) {
                    first = false;
                } else {
                    sql.append(",");
                }
                sql.append(columnMeta.columnName);
                sql.append("=? ");
            }
        }
        sql.append(" WHERE ");
        generatePartitionCondition(sql);
        sql.append(this.idColumnMeta.columnName);
        sql.append("=?");

        this.updateSql = sql.toString();
    }

    public String getDeleteSql() {
        if (deleteSql == null) {
            initDeleteSql();
        }
        return deleteSql;
    }

    private synchronized void initDeleteSql() {
        StringBuilder sql = new StringBuilder("DELETE FROM ");
        sql.append(this.getTableName());
        sql.append(" WHERE ");
        generatePartitionCondition(sql);
        sql.append(this.idColumnMeta.columnName);
        sql.append("=?");
        this.deleteSql = sql.toString();
    }


    private void generatePartitionCondition(StringBuilder sql) {
        if (this.partitionColumn != null) {
            sql.append(this.partitionColumn.columnName);
            sql.append("=? and ");
        }
    }

    public String getFindByIdSql() {
        if (findByIdSql == null) {
            findByIdSql = initFindByIdSql(false);
        }
        return findByIdSql;
    }

    public String getFindByPartitionIdSql() {
        if (findByPartitionIdSql == null) {
            findByPartitionIdSql = initFindByIdSql(true);
        }
        return findByPartitionIdSql;
    }

    private synchronized String initFindByIdSql(boolean partition) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ");
        sql.append(this.getTableName());
        sql.append(" WHERE ");
        if (partition) {
            generatePartitionCondition(sql);
        }
        sql.append(this.idColumnMeta.columnName);
        sql.append("=? limit 1");
        return sql.toString();
    }


    private static final Map<Class<?>, ModelMeta> modelMetaCache = new ConcurrentHashMap<>(cachedClassNum);

    public static ModelMeta getModelMeta(Class<?> modelCls) {
        ModelMeta modelMeta = modelMetaCache.get(modelCls);
        if (modelMeta == null) {
            synchronized (ModelMeta.class) {
                if (modelMetaCache.get(modelCls) == null) {
                    modelMetaCache.put(modelCls, new ModelMeta(modelCls));
                }
            }
            modelMeta = modelMetaCache.get(modelCls);
        }
        return modelMeta;
    }

    private ModelMeta(Class<?> modelCls) {
        this.modelCls = modelCls;
        javax.persistence.Table table = modelCls.getAnnotation(javax.persistence.Table.class);
        tableName = StringUtil.underscoreName(modelCls.getSimpleName());
        tableSchema = "";
        if (table != null) {
            if (!StringUtil.isEmpty(table.name())) {
                tableName = table.name();
            }
            tableSchema = table.schema();
        }
        Cacheable cacheable = modelCls.getAnnotation(Cacheable.class);
        if (cacheable != null) {
            this.cacheable = true;
            String key = cacheable.key();
            if (StringUtil.isEmpty(key)) {
                key = tableName;
            }
            if (registeredKeys.contains(key)) {//key不能重复
                throw new RuntimeException(tableName + " key is Repeated");
            }
            registeredKeys.add(key);
            this.key = BinaryUtil.toBytes(key + ':');
        }
        columnMetaList = getColumnMetaList();
        getCacheColumnList();

    }

    public Class<?> getModelCls() {
        return modelCls;
    }

    public String getTableName() {
        return tableName;
    }

    public String getTableSchema() {
        return tableSchema;
    }

    public List<ModelColumnMeta> getColumnMetaSet() {
        return columnMetaList;
    }

    public Iterator<ModelColumnMeta> iterateColumnMetas() {
        return columnMetaList.iterator();
    }

    public ModelColumnMeta getIdColumnMeta() {
        return idColumnMeta;
    }


    public Map<String, String> getColumnToPropertyOverrides() {
        Map<String, String> overrides = new HashMap<String, String>();
        for (ModelColumnMeta modelColumnMeta : getColumnMetaSet()) {
            overrides.put(modelColumnMeta.columnName.toLowerCase(), modelColumnMeta.fieldName);
        }
        return overrides;
    }


    public FieldAccessor getIdAccessor() {
        if (idColumnMeta == null) {
            return null;
        }
        return idColumnMeta.fieldAccessor;
    }

    public byte[] getKey() {
        return key;
    }

    public ModelColumnMeta getPartitionColumn() {
        return this.partitionColumn;
    }

    public String getIdName() {
        return this.idColumnMeta.columnName;
    }

    public boolean isCacheable() {
        return cacheable;
    }

    public List<String> getCacheColumnList() {
        if (cacheColumnList == null) {
            cacheColumnList = new ArrayList<>();
            for (ModelColumnMeta columnMeta : this.columnMetaList) {
                if (!columnMeta.isId) {
                    CacheField cacheField = columnMeta.fieldAccessor.getPropertyAnnotation(CacheField.class);
                    if (cacheField != null) {
                        cacheColumnList.add(columnMeta.columnName);
                    }

                }
            }
            Collections.sort(cacheColumnList);
            for (ModelColumnMeta columnMeta : this.columnMetaList) {
                int index = cacheColumnList.indexOf(columnMeta.columnName);
                if (index >= 0) {
                    columnMeta.cacheOrder = BinaryUtil.toBytes(index);
                }
            }
        }
        return cacheColumnList;
    }

    /**
     * translate fields to bytes user the field order
     *
     * @param fields
     * @return
     */
    public byte[][] convertToBytes(String... fields) {
        byte[][] byteFields = new byte[fields.length][];
        for (int i = 0, j = fields.length; i < j; i++) {
            int index = cacheColumnList.indexOf(fields[i]);
            if (index < 0) {
                return null;
            }
            byteFields[i] = BinaryUtil.toBytes(index);
        }
        return byteFields;
    }

    public String[] convertToFields(byte[]... bytes) {
        String[] fields = new String[bytes.length];
        for (int i = 0, j = bytes.length; i < j; i++) {
            String field = cacheColumnList.get(bytes[i][0]);
            if (field == null) {
                return null;
            }
            fields[i] = field;
        }
        return fields;
    }

    public String[] getCachedFields() {
        String[] res = new String[cacheColumnList.size()];
        int i = 0;
        for (ModelColumnMeta columnMeta : columnMetaList) {
            if (columnMeta.cacheOrder != null) {
                res[i++] = columnMeta.fieldName;
            }
        }
        return res;
    }

}
