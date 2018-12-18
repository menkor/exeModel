package org.exemodel.orm;

import org.exemodel.cache.ICache;
import org.exemodel.cache.Promise;
import org.exemodel.exceptions.JdbcRuntimeException;
import org.exemodel.session.AbstractSession;
import org.exemodel.session.Session;
import org.exemodel.builder.SqlBuilder;
import org.exemodel.util.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author zp [15951818230@163.com]
 */
@SuppressWarnings("unchecked")
public class Statement<T> extends SqlBuilder<T> {

    protected Class<?> modelClass;
    protected volatile ModelMeta modelMeta;
    protected Object key;

    public Statement(Class<?> clazz) {
        this.modelClass = clazz;
    }

    protected ModelMeta getModelMeta() {
        if (modelMeta == null) {
            synchronized (this) {
                modelMeta = ModelMeta.getModelMeta(modelClass);
            }
        }
        return modelMeta;
    }

    public Statement() {
    }

    protected void setModelClass(Class<? extends ExecutableModel> modelClass) {
        this.modelClass = modelClass;
    }

    protected ICache getCache() {
        return getSession().getCache();
    }

    protected boolean isCacheable() {
        return getModelMeta().isCacheable();
    }

    public static Session getSession() {
        return AbstractSession.currentSession();
    }

    public T id(Object value) {
        key = value;
        return eq("id", value);
    }

    public <E> E findOneByNativeSql(String sql, Object... params) {
        return (E) getSession().findOneByNativeSql(this.modelClass, sql, params);
    }

    public <E> List<E> findListByNativeSql(String sql, Object... params) {
        return (List<E>) getSession().findListByNativeSql(this.modelClass, sql, params);
    }

    public <E> E findOneByNativeSql(String sql, ParameterBindings parameterBindings) {
        return (E) getSession().findOneByNativeSql(this.modelClass, sql, parameterBindings);
    }

    public <E> List<E> findListByNativeSql(String sql, ParameterBindings parameterBindings) {
        return (List<E>) getSession().findListByNativeSql(this.modelClass, sql, parameterBindings);
    }

    public <E> E findById(Object id) {
        return findById(id, null);
    }

    public <E> E findById(Object id, Object partitionId) {
        return (E) getSession().find(modelClass, id, partitionId);
    }


    public <E> E selectOne(String... fields) {
        return selectOne(this.modelClass, fields);
    }

    public int count() {
        String sql = " SELECT count(id) FROM " + getModelMeta().getTableName() + where;
        List<Long> res = getSession().findListByNativeSql(Long.class, sql, parameterBindings);
        if (res == null || res.size() == 0) {
            return 0;
        }
        if (res.size() == 1) {
            return res.get(0).intValue();
        }
        return res.size();
    }


    public <E> List<E> selectByPagination(Pagination pagination, String... fields) {
        String sql = findList(getModelMeta().getTableName(), fields);
        return (List<E>) getSession().findListByNativeSql(this.modelClass, sql, parameterBindings, pagination);
    }


    public <E> List<E> selectByPagination(Class<E> modelClass, Pagination pagination, String... fields) {
        String sql = findList(getModelMeta().getTableName(), fields);
        return getSession().findListByNativeSql(modelClass, sql, parameterBindings, pagination);
    }

    public <E> List<E> selectList(String... fields) {
        return this.selectList(this.modelClass, fields);
    }

    public <E> List<E> selectList(Class<?> targetClass, String... fields) {
        fields = fillColumns(fields, targetClass);
        String sql = findList(getModelMeta().getTableName(), fields);
        return (List<E>) getSession().findListByNativeSql(targetClass, sql, parameterBindings);
    }

    public <E> E selectOne(final Class targetClass, String... fields) {
        fields = fillColumns(fields, targetClass);
        ModelMeta modelMeta = getModelMeta();
        ModelMeta resultModelMetal = ModelMeta.getModelMeta(targetClass);
        if (isCacheable() && key != null && fields.length != 0) {
            byte[][] bytes = modelMeta.convertToBytes(StringUtil.underscoreNames(fields));
            if (bytes != null) {
                final String sql = findOne(modelMeta.getTableName(), modelMeta.getCachedFields());
                final Object[] sqlParams = parameterBindings.getIndexParametersArray();
                if (getSession().isInCacheBatch()) {
                    try {
                        ExecutableModel res = (ExecutableModel) targetClass.newInstance();
                        Promise promise = new Promise(res) {
                            @Override
                            public Object onFail() {
                                return getSession().findOneByNativeSql(targetClass, sql, sqlParams);
                            }
                        };
                        promise.setFields(fields);
                        promise.setModelMeta(resultModelMetal);
                        getCache().get(key, targetClass, promise, bytes, fields);
                        return (E) promise.getResult();
                    } catch (Exception e) {
                        throw new JdbcRuntimeException(e);
                    }
                }

                Object cached = getCache().get(key, targetClass, null, bytes, fields);
                if (cached != null) {
                    return (E) cached;
                }
                ExecutableModel fromDb = (ExecutableModel) getSession().findOneByNativeSql(this.modelClass, sql, sqlParams);
                //insert the whole cached org.exemodel.entity
                if (fromDb != null) {
                    FieldAccessor fieldAccessor = modelMeta.getIdAccessor();
                    fieldAccessor.setProperty(fromDb, key);
                    getCache().save(fromDb);
                    try {
                        Object res = targetClass.newInstance();
                        fromDb.copyPropertiesTo(res);
                        return (E) res;
                    } catch (Exception e) {
                        throw new JdbcRuntimeException(e);
                    }
                } else {
                    return null;
                }
            }
        }
        return (E) getSession().findOneByNativeSql(targetClass, findOne(modelMeta.getTableName(), fields),
                parameterBindings);
    }

    /**
     * 以下几种set方法用于更新时
     *
     * @param keyValues columnName,value,columnName,value的形式
     * @return 更新的行数
     */
    public int set(Object... keyValues) {
        if (keyValues == null || keyValues.length == 0 || keyValues.length % 2 != 0) {
            throw new JdbcRuntimeException("Error update set");
        }
        ParameterBindings pb = new ParameterBindings();
        StringBuilder sb = new StringBuilder(" UPDATE ");
        sb.append(getModelMeta().getTableName());
        sb.append(" SET ");
        int i = 0;
        for (Object o : keyValues) {
            if (i % 2 == 0) {
                if (i != 0) {
                    sb.append(",");
                }
                sb.append(StringUtil.underscoreName((String) o));
                sb.append("=?");
            } else {
                pb.addIndexBinding(o);
            }
            i++;
        }
        sb.append(where);
        ParameterBindings all = pb.addAll(parameterBindings);
        int res = getSession().executeUpdate(sb.toString(), all);
        if (isCacheable() && res > 0) {
            updateCache(keyValues);
        }
        return res;
    }


    public int set(Map<String, Object> map) {
        if (map == null || map.size() == 0) {
            throw new JdbcRuntimeException("Error update map");
        }
        ParameterBindings pb = new ParameterBindings();
        StringBuilder sb = new StringBuilder(" UPDATE ");
        sb.append(getModelMeta().getTableName());
        sb.append(" SET ");
        boolean init = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (init) {
                init = false;
            } else {
                sb.append(',');
            }
            sb.append(StringUtil.underscoreName(entry.getKey()));
            sb.append("=?");
            pb.addIndexBinding(entry.getValue());
        }
        sb.append(where);
        ParameterBindings all = pb.addAll(parameterBindings);
        int res = getSession().executeUpdate(sb.toString(), all);
        if (isCacheable() && res > 0) {
            int i = 0;
            Object[] keyValues = new Object[map.size() * 2];
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                keyValues[i++] = entry.getKey();
                keyValues[i++] = entry.getValue();
            }
            updateCache(keyValues);
        }
        return res;
    }

    public int setByObject(Object from) {
        ModelMeta meta = ModelMeta.getModelMeta(from.getClass());
        ModelMeta thisMeta = getModelMeta();
        ParameterBindings pb = new ParameterBindings();
        StringBuilder sb = new StringBuilder(" UPDATE ");
        sb.append(thisMeta.getTableName());
        sb.append(" SET ");
        boolean init = true;
        List<Object> keyValues = new ArrayList<>();
        for (ModelMeta.ModelColumnMeta modelColumnMeta : meta.getColumnMetaSet()) {
            if (modelColumnMeta.isId ||
                    modelColumnMeta.fieldName.equals(thisMeta.getIdColumnMeta().fieldName)
                    || !getModelMeta().existColumn(modelColumnMeta.columnName)) {
                continue;
            }

            FieldAccessor fieldAccessor = modelColumnMeta.fieldAccessor;
            Object value = fieldAccessor.getProperty(from);
            if (value == null) {
                continue;
            }
            if (init) {
                init = false;
            } else {
                sb.append(',');
            }
            keyValues.add(modelColumnMeta.fieldName);
            keyValues.add(value);
            sb.append(modelColumnMeta.columnName);
            sb.append("=?");
            pb.addIndexBinding(value);
        }
        sb.append(where);
        String sql = sb.toString();
        ParameterBindings all = pb.addAll(parameterBindings);
        int rs = getSession().executeUpdate(sql, all);
        if (isCacheable() && rs > 0) {
            Object[] objects = new Object[keyValues.size()];
            objects = keyValues.toArray(objects);
            updateCache(objects);
        }
        return rs;
    }

    public int set(String setSql, ParameterBindings setParams) {
        if (isCacheable()) {
            throw new JdbcRuntimeException("WARN: You update a cacheable org.exemodel.entity and the cache field not update");
        }
        String sql = " UPDATE " + getModelMeta().getTableName() + " SET " + setSql + where;
        ParameterBindings all;
        if (setParams == null) {
            setParams = new ParameterBindings();
        }
        all = setParams.addAll(parameterBindings);
        return getSession().executeUpdate(sql, all);
    }

    /**
     * remove with conditions
     */
    public int remove() {
        if (isCacheable()) {
            if (key == null) {
                throw new JdbcRuntimeException("Can't remove a cacheable org.exemodel.entity without id");
            }
            getCache().delete(BinaryUtil.generateKey(getModelMeta().getKey(), BinaryUtil.getBytes(key)));
        }
        return getSession()
                .executeUpdate(" DELETE FROM " + getModelMeta().getTableName() + where, parameterBindings);
    }

    public int execute(String sql, ParameterBindings parameterBindings1) {
        return getSession().executeUpdate(sql, parameterBindings1);
    }

    /**
     * check  meet the conditions
     **/

    public boolean exists() {
        String sql = " SELECT 1 FROM " + getModelMeta().getTableName() + where + " limit 1";
        return
                AbstractSession.currentSession()
                        .findOneByNativeSql(Integer.class, sql, parameterBindings) != null;
    }


    @Deprecated
    public <E> E findCache(Object id) {
        return this.findCache(id, null);
    }

    @Deprecated
    public <E> E findCache(Object id, Object partitionId) {
        ModelMeta modelMeta = getModelMeta();
        int cachedLength = modelMeta.getCacheColumnList().size();
        byte[][] cacheOrders = new byte[cachedLength][];
        String[] fields = new String[cachedLength];
        int i = 0;
        for (ModelMeta.ModelColumnMeta columnMeta : modelMeta.getColumnMetaSet()) {
            if (columnMeta.cacheOrder != null) {
                cacheOrders[i] = columnMeta.cacheOrder;
                fields[i] = columnMeta.fieldName;
                i++;
            }
        }
        if (getSession().isInCacheBatch()) {
            try {
                final ParameterBindings parameterBindings = new ParameterBindings();
                final String sql = getFindSql(id, partitionId, parameterBindings);
                ExecutableModel res = (ExecutableModel) this.modelClass.newInstance();
                Promise promise = new Promise(res) {
                    @Override
                    public Object onFail() {
                        return getSession()
                                .findOneByNativeSql(modelClass, sql, parameterBindings.getIndexParametersArray());
                    }
                };
                promise.setModelMeta(modelMeta);
                promise.setFields(fields);
                getCache().get(id, modelClass, promise, cacheOrders, fields);
                return (E) res;
            } catch (Exception e) {
                throw new JdbcRuntimeException(e);
            }
        }
        Object cached = getCache().get(id, modelClass, null, cacheOrders, fields);
        if (cached != null) {
            return (E) cached;
        }
        ParameterBindings parameterBindings = new ParameterBindings();
        final String sql = getFindSql(id, partitionId, parameterBindings);
        ExecutableModel fromDb = (ExecutableModel) getSession().findOneByNativeSql(modelClass, sql, parameterBindings);
        if (fromDb != null) {
            FieldAccessor fieldAccessor = modelMeta.getIdAccessor();
            fieldAccessor.setProperty(fromDb, id);
            getCache().save(fromDb);
        }
        return (E) fromDb;
    }


    private String getFindSql(Object id, Object partitionId, ParameterBindings parameterBindings) {
        ModelMeta thisMeta = getModelMeta();
        ModelMeta.ModelColumnMeta partitionColumn = thisMeta.getPartitionColumn();
        ModelMeta.ModelColumnMeta idColumn = thisMeta.getIdColumnMeta();
        StringBuilder sb = new StringBuilder(" SELECT ");
        sb.append(StringUtil.join(thisMeta.getCacheColumnList(), ","));
        sb.append(" FROM ");
        sb.append(thisMeta.getTableName());
        sb.append(" WHERE ");
        if (partitionId != null && partitionColumn != null) {
            sb.append(partitionColumn.columnName);
            sb.append(" = ? and ");
            parameterBindings.addIndexBinding(partitionId);
        }
        sb.append(idColumn.columnName);
        sb.append(" = ? limit 1");
        parameterBindings.addIndexBinding(id);
        return sb.toString();
    }


    /**
     * update the field if cached in redis
     *
     * @param keyValues eg. id,100,name,zp
     */
    private void updateCache(Object... keyValues) {
        Object id = key;
        List<String> cacheIndex = getModelMeta().getCacheColumnList();
        if (id != null) {
            int index = 0;
            byte[][] bytes = new byte[keyValues.length + 1][];
            for (int i = 0, l = keyValues.length; i < l; i++) {
                if (i % 2 == 0) {//key
                    int order = cacheIndex.indexOf(StringUtil.underscoreName((String) keyValues[i]));
                    if (order < 0) {
                        continue;
                    }
                    bytes[index++] = BinaryUtil.toBytes(order);
                    i++;
                    bytes[index++] = BinaryUtil.getBytes(keyValues[i]);
                }
            }
            if (index > 0) {
                byte[][] updateCachedFields = new byte[index + 1][];
                updateCachedFields[0] = BinaryUtil.generateKey(getModelMeta().getKey(), BinaryUtil.getBytes(id));
                System.arraycopy(bytes, 0, updateCachedFields, 1, index);
                getCache().update(updateCachedFields);
            }
        } else {
            for (int i = 0, l = keyValues.length; i < l; i++) {
                if (i % 2 == 0) {//key
                    int order = cacheIndex.indexOf(StringUtil.underscoreName((String) keyValues[i]));
                    if (order > -1) {//update cached field
                        throw new JdbcRuntimeException("Update cached fields but not with id");
                    }
                }
            }
        }
    }

    private String findOne(String table, String[] fields) {
        return " SELECT " + StringUtil.joinParams(",", fields) + " FROM " + table + where + " limit 1";
    }

    private String findList(String table, String[] fields) {
        return " SELECT " + StringUtil.joinParams(",", fields) + " FROM " + table + where;
    }

    private boolean isEmpty(String[] fields) {
        return fields == null || fields.length == 0;
    }

    private String[] generateFields(Class targetClass) {
        ModelMeta modelMeta = ModelMeta.getModelMeta(targetClass);
        List<String> res = new ArrayList<>();
        for (ModelMeta.ModelColumnMeta columnMeta : modelMeta.getColumnMetaSet()) {
            if (getModelMeta().existColumn(columnMeta.columnName)) {
                res.add(columnMeta.columnName);
            }
        }
        return res.toArray(new String[res.size()]);
    }

    private String[] fillColumns(String[] fields, Class targetClass) {
        if (fields == null || fields.length == 0) {
            if (targetClass != modelClass) {
                List<String> res = new ArrayList<>();
                for(ModelMeta.ModelColumnMeta columnMeta: ModelMeta.getModelMeta(targetClass).getColumnMetaSet()){
                    if(getModelMeta().existColumn(columnMeta.columnName)){
                        res.add(columnMeta.columnName);
                    }
                }
                return res.toArray(new String[]{});
            }
            return new String[]{"*"};
        }
        if (modelClass == targetClass || ModelMeta.getModelMeta(targetClass).existColumn(getModelMeta().getIdName())) {
            for (String t : fields) {
                if (t.equals(getModelMeta().getIdName())) {
                    return fields;
                }
            }
            String[] res = Arrays.copyOf(fields, fields.length + 1);
            res[fields.length] = getModelMeta().getIdName();
            return res;
        }


        return fields;
    }
}
