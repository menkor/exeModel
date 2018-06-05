package org.exemodel.orm;

import org.exemodel.annotation.CacheField;
import org.exemodel.annotation.Cacheable;
import org.exemodel.annotation.PartitionId;
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
    private boolean cacheable = false;
    private boolean isAllCached = false;
    private byte[] key = null;
    private List<ModelColumnMeta> columnMetaList;
    private ModelColumnMeta idColumnMeta;
    private ModelColumnMeta partitionColumn;
    private volatile List<String> cacheColumnList;//{index:fieldName}
    private volatile Map<String, FieldAccessor> accessorMap;
    /**
     * column info of orm org.exemodel.entity class, ignore all fieldNameBytes with @javax.sql.Transient
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


    private static final Map<Class<?>, ModelMeta> modelMetaCache = new ConcurrentHashMap<>(cachedClassNum);

    public static ModelMeta getModelMeta(Class<?> modelCls) {
        ModelMeta modelMeta = modelMetaCache.get(modelCls);
        if (modelMeta == null) {
            synchronized (ModelMeta.class) {
                modelMeta = modelMetaCache.get(modelCls);
                if (modelMeta == null) {
                    modelMeta = new ModelMeta(modelCls);
                    modelMetaCache.put(modelCls, modelMeta);
                }
            }
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
            this.isAllCached = cacheable.all();
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
            synchronized (this) {
                if (cacheColumnList == null) {
                    cacheColumnList = new ArrayList<>();
                    for (ModelColumnMeta columnMeta : this.columnMetaList) {
                        if (!columnMeta.isId) {
                            if(isAllCached){
                                cacheColumnList.add(columnMeta.columnName);
                            }else{
                                CacheField cacheField = columnMeta.fieldAccessor.getPropertyAnnotation(CacheField.class);
                                if (cacheField != null) {
                                    cacheColumnList.add(columnMeta.columnName);
                                }
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
                    this.isAllCached = cacheColumnList.size()==(columnMetaList.size()-1);
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


    public Map<String, FieldAccessor> getAccessorMap() {
        if (accessorMap == null) {
            synchronized (this) {
                if (accessorMap == null) {
                    accessorMap = new HashMap<>();
                    for (ModelColumnMeta columnMeta : columnMetaList) {
                        accessorMap.put(columnMeta.columnName, columnMeta.fieldAccessor);
                    }
                }

            }
        }
        return accessorMap;
    }

    public boolean existColumn(String column){
        for (ModelColumnMeta columnMeta : columnMetaList) {
            if(columnMeta.columnName.equals(column)){
                return true;
            }
        }
        return false;
    }

    public boolean isAllCached(){
        return isAllCached;
    }


}
